<script>
export default {
  onLaunch() {},
  onShow() {},
  onHide() {},
}
</script>

<style lang="scss">
@import './uni.scss';

page {
  background-color: #f5f6fa;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
}

.container {
  padding: 24rpx;
  min-height: 100vh;
  box-sizing: border-box;
}

.page-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1a2a6c;
  margin-bottom: 24rpx;
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 16rpx rgba(0,0,0,0.06);
}
.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #303133;
  margin-bottom: 20rpx;
  padding-bottom: 16rpx;
  border-bottom: 2rpx solid #f0f0f0;
}

.btn-primary {
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff;
  border-radius: 12rpx;
  font-size: 28rpx;
  padding: 20rpx 0;
  text-align: center;
  border: none;
  width: 100%;
}
.btn-secondary {
  background: #f5f7fa;
  color: #606266;
  border-radius: 12rpx;
  font-size: 28rpx;
  padding: 20rpx 0;
  text-align: center;
  border: 2rpx solid #dcdfe6;
  width: 100%;
}
.btn-danger {
  background: linear-gradient(135deg, #f56c6c, #e63946);
  color: #fff;
  border-radius: 12rpx;
  font-size: 28rpx;
  padding: 20rpx 0;
  text-align: center;
  border: none;
}
.btn-warning {
  background: linear-gradient(135deg, #e6a23c, #f5a623);
  color: #fff;
  border-radius: 12rpx;
  font-size: 28rpx;
  padding: 20rpx 0;
  text-align: center;
  border: none;
}
.btn-success {
  background: linear-gradient(135deg, #11998e, #38ef7d);
  color: #fff;
  border-radius: 12rpx;
  font-size: 28rpx;
  padding: 20rpx 0;
  text-align: center;
  border: none;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 16rpx;
  border-radius: 6rpx;
  font-size: 22rpx;
  font-weight: 500;
}
.tag-success { background: #f0f9eb; color: #67c23a; }
.tag-primary  { background: #ecf5ff; color: #409eff; }
.tag-warning  { background: #fdf6ec; color: #e6a23c; }
.tag-danger   { background: #fef0f0; color: #f56c6c; }
.tag-info     { background: #f4f4f5; color: #909399; }

.empty-box {
  text-align: center;
  padding: 80rpx 40rpx;
  color: #c0c4cc;
  font-size: 28rpx;
}
.empty-box .empty-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
  display: block;
}

.loading-box {
  text-align: center;
  padding: 60rpx;
  color: #c0c4cc;
  font-size: 26rpx;
}

.divider { height: 2rpx; background: #f0f0f0; margin: 16rpx 0; }

.avatar-circle {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1a2a6c, #2d6a9f);
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.list-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #f0f0f0;
}
.list-item:last-child { border-bottom: none; }

/* 通用 modal */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}
.modal-box {
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  width: 100%;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32rpx 40rpx;
  border-bottom: 2rpx solid #f0f0f0;
  flex-shrink: 0;
}
.modal-title { font-size: 32rpx; font-weight: 600; color: #303133; }
.modal-close { font-size: 36rpx; color: #c0c4cc; padding: 4rpx 8rpx; }
.modal-body  { flex: 1; padding: 32rpx 40rpx; overflow: hidden; }
.modal-footer {
  display: flex;
  gap: 16rpx;
  padding: 24rpx 40rpx;
  border-top: 2rpx solid #f0f0f0;
  flex-shrink: 0;
}
.modal-footer button { flex: 1; }

/* 通用表单 */
.form-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f5f5f5;
  gap: 16rpx;
}
.form-row:last-child { border-bottom: none; }
.form-label {
  font-size: 28rpx;
  color: #606266;
  width: 160rpx;
  flex-shrink: 0;
}
.form-input {
  flex: 1;
  font-size: 28rpx;
  color: #303133;
  text-align: right;
}
.form-textarea {
  flex: 1;
  font-size: 26rpx;
  color: #303133;
  min-height: 120rpx;
  text-align: left;
  line-height: 1.6;
}
</style>
