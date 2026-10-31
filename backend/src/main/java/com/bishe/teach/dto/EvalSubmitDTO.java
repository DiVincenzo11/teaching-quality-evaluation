package com.bishe.teach.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class EvalSubmitDTO {

    @NotNull(message = "教师ID不能为空")
    private Long teacherId;

    @NotNull(message = "学期ID不能为空")
    private Long semesterId;

    @NotEmpty(message = "答题列表不能为空")
    private List<AnswerItem> answers;

    @Size(max = 200, message = "评语不超过200字")
    private String essayContent;

    /** 前置问题1：出勤率 1-5 */
    @NotNull(message = "请填写出勤率")
    private Integer attendanceRate;

    /** 前置问题2：课外投入时间 1-5 */
    @NotNull(message = "请填写课外投入时间")
    private Integer studyTime;

    @Data
    public static class AnswerItem {
        @NotNull(message = "题目ID不能为空")
        private Long    questionId;
        @NotNull(message = "答案不能为空")
        private Integer answerValue;
    }
}
