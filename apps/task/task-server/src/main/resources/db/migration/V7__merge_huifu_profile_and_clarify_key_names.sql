INSERT INTO task_merchant_profile
  (id, code, name, description, configuration_json, created_at, updated_at)
SELECT 'huifu', 'HUIFU', '汇付', '汇付商户配置', '{}', NOW(6), NOW(6)
FROM task_merchant_profile legacy
WHERE UPPER(legacy.code) = 'HUIFU_TEST'
  AND NOT EXISTS (
    SELECT 1 FROM task_merchant_profile canonical WHERE UPPER(canonical.code) = 'HUIFU'
  )
LIMIT 1;

UPDATE task_merchant_profile canonical
JOIN task_merchant_profile legacy ON UPPER(legacy.code) = 'HUIFU_TEST'
SET canonical.configuration_json = JSON_MERGE_PATCH(
      COALESCE(NULLIF(canonical.configuration_json, ''), '{}'),
      COALESCE(NULLIF(legacy.configuration_json, ''), '{}')
    ),
    canonical.name = '汇付',
    canonical.description = '汇付商户配置',
    canonical.updated_at = NOW(6)
WHERE UPPER(canonical.code) = 'HUIFU';

UPDATE task_merchant_profile
SET configuration_json = JSON_SET(
      configuration_json,
      '$.merchant_private_key',
      JSON_UNQUOTE(JSON_EXTRACT(configuration_json, '$.rsa_private_key'))
    )
WHERE UPPER(code) = 'HUIFU'
  AND JSON_CONTAINS_PATH(configuration_json, 'one', '$.rsa_private_key') = 1
  AND JSON_CONTAINS_PATH(configuration_json, 'one', '$.merchant_private_key') = 0;

UPDATE task_merchant_profile
SET configuration_json = JSON_SET(
      configuration_json,
      '$.huifu_public_key',
      JSON_UNQUOTE(JSON_EXTRACT(configuration_json, '$.rsa_public_key'))
    )
WHERE UPPER(code) = 'HUIFU'
  AND JSON_CONTAINS_PATH(configuration_json, 'one', '$.rsa_public_key') = 1
  AND JSON_CONTAINS_PATH(configuration_json, 'one', '$.huifu_public_key') = 0;

UPDATE task_merchant_profile
SET configuration_json = JSON_REMOVE(configuration_json, '$.rsa_private_key', '$.rsa_public_key'),
    name = '汇付',
    description = '汇付商户配置',
    updated_at = NOW(6)
WHERE UPPER(code) = 'HUIFU';

UPDATE task_definition task
JOIN task_merchant_profile legacy ON task.merchant_id = legacy.id AND UPPER(legacy.code) = 'HUIFU_TEST'
JOIN task_merchant_profile canonical ON UPPER(canonical.code) = 'HUIFU'
SET task.merchant_id = canonical.id,
    task.category = canonical.code,
    task.updated_at = NOW(6);

DELETE FROM task_merchant_profile
WHERE UPPER(code) = 'HUIFU_TEST';
