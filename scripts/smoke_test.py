"""Exercise the demo API against a disposable database (creates a test student/evaluation)."""
import argparse
import json
import secrets
import urllib.error
import urllib.request
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:8080/api')
    parser.add_argument('--demo-data', action='store_true', required=True,
                        help='Confirm this service uses disposable demo data, not an existing database.')
    args = parser.parse_args()

    def call(path, token=None, data=None, expected_http=200, expected_code=200):
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        request = urllib.request.Request(args.base_url.rstrip('/') + path,
            data=json.dumps(data, ensure_ascii=False).encode('utf-8') if data is not None else None,
            headers=headers)
        try:
            response = urllib.request.urlopen(request, timeout=60)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            payload = json.load(response)
            if response.status != expected_http or payload.get('code') != expected_code:
                raise AssertionError(f'{path}: HTTP {response.status}, code={payload.get("code")}, '
                                     f'message={payload.get("message")}')
            return payload.get('data')

    tokens = {}
    for name, role in [('admin', 3), ('demo_teacher', 2), ('demo_student', 1)]:
        tokens[name] = call('/auth/login', data={
            'username': name, 'password': '123456', 'roleType': role})['token']
    print('PASS: three demo roles can log in')
    call('/admin/dashboard/overview', expected_http=401, expected_code=401)
    call('/admin/dashboard/overview', tokens['demo_student'], expected_http=403, expected_code=403)
    call('/auth/login', data={'username': 'demo_student', 'password': '123456', 'roleType': 3},
         expected_code=400)
    print('PASS: unauthenticated access, cross-role access and wrong login role are rejected')

    student = tokens['demo_student']
    semesters = call('/common/semesters', student)
    sid = next(x['id'] for x in semesters if x['isCurrent'] == 1)
    teachers = call(f'/teacher/list/for-student?semesterId={sid}', student)
    teacher_id = next(x['userId'] for x in teachers if x['username'] == 'demo_teacher')
    questions = [x for x in call('/questions', student) if x['questionType'] == 1]
    assert len(questions) == 12, f'Expected 12 choice questions, got {len(questions)}'
    configs = call('/admin/pre-question-config', tokens['admin'])
    assert len(configs) == 10
    print('PASS: class/teacher links, questionnaire and engagement configuration load')

    # Unique student makes repeated smoke runs possible without deleting existing records.
    username = 'smoke_' + uuid.uuid4().hex[:12]
    registered = call('/auth/register', data={
        'username': username, 'password': secrets.token_hex(8), 'realName': '演示测试学生',
        'roleType': 1, 'studentNo': username, 'className': '软件工程2班',
        'major': '软件工程', 'grade': '2022'})
    token = registered['token']
    teacher = tokens['demo_teacher']
    before = call(f'/teacher/eval/stats?semesterId={sid}', teacher)['evalCount']
    submission = {'teacherId': teacher_id, 'semesterId': sid, 'attendanceRate': 5, 'studyTime': 4,
        'essayContent': '老师认真负责，讲解清晰，课堂内容丰富。',
        'answers': [{'questionId': q['id'], 'answerValue': 5} for q in questions]}
    call('/student/eval/submit', token, submission)
    call('/student/eval/submit', token, submission, expected_code=400)
    history = call('/student/eval/history', token)
    assert len(history) == 1 and float(history[0]['totalScore']) == 100.0, history
    details = call(f'/student/eval/record/detail?teacherId={teacher_id}&semesterId={sid}', token)
    assert len(details['answers']) == 12
    after = call(f'/teacher/eval/stats?semesterId={sid}', teacher)['evalCount']
    assert after == before + 1, f'Teacher statistics cache stale: before={before}, after={after}'
    print('PASS: submission, scoring, duplicate prevention, saved answers and immediate teacher statistics')

    call('/teacher/feedback', teacher, {'semesterId': sid,
        'reflection': '演示反馈：改进课堂互动。', 'improvementPlan': '增加实践案例。'})
    assert call('/teacher/feedback', teacher)
    assert call(f'/student/feedback/list?semesterId={sid}', token)
    overview = call('/admin/dashboard/overview', tokens['admin'])
    assert overview['evalCount'] >= 1
    call(f'/admin/analysis/teacher-stats?semesterId={sid}', tokens['admin'])
    call(f'/admin/analysis/dynamic-weights?semesterId={sid}', tokens['admin'])
    print('PASS: teacher feedback, student feedback view and administrator statistics')

    call('/auth/logout', token, {})
    call('/auth/info', token, expected_http=401, expected_code=401)
    print('PASS: logout invalidates the issued token')
    print('ALL SMOKE CHECKS PASSED')


if __name__ == '__main__':
    main()
