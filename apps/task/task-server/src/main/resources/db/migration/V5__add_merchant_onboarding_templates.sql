INSERT INTO task_template
  (id, display_name, description, category, class_name, dangerous, created_at, updated_at)
VALUES
  ('merchant-enterprise-onboarding', '企业商户进件', '根据企业商户进件接口字段填写申请参数。', '商户进件类', 'com.lab.taskexecutor.controller.onboarding.EnterpriseMerchantOnboardingTask', TRUE, NOW(6), NOW(6)),
  ('merchant-individual-onboarding', '个人商户进件', '根据个人商户进件接口字段填写申请参数。', '商户进件类', 'com.lab.taskexecutor.controller.onboarding.IndividualMerchantOnboardingTask', TRUE, NOW(6), NOW(6)),
  ('merchant-business-open', '商户业务开通', '为已进件商户提交业务开通参数。', '商户进件类', 'com.lab.taskexecutor.controller.onboarding.MerchantBusinessOpenTask', TRUE, NOW(6), NOW(6)),
  ('merchant-picture-upload', '图片上传', '上传商户进件所需图片并返回文件标识。', '商户进件类', 'com.lab.taskexecutor.controller.onboarding.MerchantPictureUploadTask', TRUE, NOW(6), NOW(6)),
  ('merchant-application-status-query', '申请单状态查询', '按申请单号查询商户进件申请状态。', '商户进件类', 'com.lab.taskexecutor.controller.onboarding.MerchantApplicationStatusQueryTask', TRUE, NOW(6), NOW(6));
