package com.platform.task.controller.dji;

import com.huifu.bspay.sdk.opps.core.config.MerConfig;
import com.platform.task.controller.util.TaskExecutionContext;

/**
 * 大疆商户公共配置
 */
public class DjiConfig {
    public static final String PRODUCT_ID      = "EDUSTD";
    public static final String SYS_ID          = "6666000194232902";
    public static final String UPPER_HUIFU_ID  = "6666000108840829";

    public static final String RSA_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCmZHZo+9DZEY4JhvIEwQFN0GdhVyl9FgoYR15MCLoprFaSwjrGTKMedmi8ejnA5rXlRHWcyDYfsg2/kZ5ddVRXyk9z+qcgSMbx/SfOxppTJryn4FjOZ6zT51nNY0h8RdNzZ7q8C1S9yOpGx/yzFu6tT5em0o/8YkL+10EQD/fx7bvPMnzwtUika/RzOrZ/WpSJJy2f8nQjDxdj8OfDlMEatx9IJhtwHRlk+wmTf7P4IAoFeG4OmidnYdAn9p96ObCl+8zfwAf1BEWIGqbylvSqe7DS/mJ6AirnoMCti2SYZu5xFj1oLTJpmCBbn/eZpmIUzrBN1S9u7/OVK2gaPWijAgMBAAECggEAPXRLW3jt6M4YBKLHjkPuhFvdYtE7bIyQS+lIBZMPlsak7u03ofe6I00eX8ZQwFM1iSjgu3girVAozcEtmVz1Orto3vALe9wFRmj2o+MsKlvbP7GXexxFc7o1q6rooaV0dGIyULNqp/GlvWCtj0OAAZis7RtFvLRaBB9iU1lifQJzCZ6uPWokJKRx/lRqbVs6DvrJRTk0g7hxnc/rITsRB/jpHtVMy2UmMAtqGfGgkzlEoZ7oWPzNWpdFpYco1G1InAc8kQNYoWCRFTUeJfr26SLeGhrWLUHLRuJKHWdysX2uzXKpE78lyw6ZYIqIcpDCvoRoIe7poN5gTawxvVeLwQKBgQD2YzmBjlS3yG8XIje0Bjzc59xmx5+/ggRsaJsByMZNhtzCdJy8OfLQx+A+L5rKInVkcOa1J+q+DR14WfSsztoob3KtA7va6ary5pa0VRVyiK8XwGPZw3VPCbsU0cNd5LuPW8HY4wyhUMnQ6MeW9tpRe+YdVDYPzlrldAJGJ8D9YQKBgQCs4kr+BwEZGGS9DOrCvhk6gomr6DFvaxpT75cUtFAq1/8y76pKFVOGcNuGfgKQXv3EhyGwBKLCNfOqDGDVv18uxAcCGFLN3D1oMBe/hR2dtspPmWZLaWVva6gAMohzaqLKeVWilpNmGhpGiAJobfzBRZuVCKvUkcPHkwfdnlPAgwKBgGTN0dBEqDqM4Y6IbFvWFX6Xyh+u2pfinOaxoYGIYEGFxLo2NY3483Qh4ofuuBitInRWkL1bOHpLKVx9CVLOSokyl6tblLcK1OsOFmhvSxgR/fjsuK12f383zvfEnnbCx03uz49pzVgOkpNLOaV1F3sxAsPLGVc8KQopfWiwiQ/hAoGBAIiX8rBqUE/HmjBUOFfNYpl78dJFDmn9sTuIHVJadMylBBj2ixElcGzUVl4YyWXP56iTK+aqgv33KqG8TfYT0dxdPTxGg4Xq8QmUle7X+eJWPdpOVShYCbDS2lXtlym5ow1eeG6Rkbwl+4SmiWCBJiZ+HMLPqxO0Z38TiC9tnLaJAoGAPjZ/rlqE4W07e20jTYwiY4/fAHxRQ6bjIlTj44yHnYapvLXUOQpfSUB5ECnAplra2+QMu1mabrT/Xu3O1FwrBmqnGa8CZ8+iSPdjU/S4lx9MChNxvfdGDO5JxP6XXZS8BUKTIc65YfKv7NhC5CR/+tRYoaP9Bmzw2AE9LYjHIjE=";
    public static final String RSA_PUBLIC_KEY  = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwCvpdUMFNCqwwgsyHOBAjIcfz7CCRVbSzO7WdxYsr7qLPRLLfRcUV4I39QEOQY3NLcfPrMM2oiYfaRAvl1TogPn894YfIW5UWm29pMSGJWF718fTod/C++gRNLj9fNr2nyfx8xePBuCXtRmUBe2Nyxk9899+Qf9dhhryN3bP2/26GKWLukKytZqVrlAqVuFvzgykWdAVxxCqZ5tLDlfSI0Ju9xLu2AKS2kRKrJn8AwmIldTb3M64STeE+s23ls7QWYH4ZKXHWYD3lPukBhSdVxX2GHk84VRZHiB7rhoK1lYCHTlEM2pgFVLbu0uhOwGYHJ4aCuJIUcOARAFGrN6MvQIDAQAB";

    private DjiConfig() {}

    /** Uses the selected platform merchant profile while retaining legacy direct-main defaults. */
    static MerConfig merConfig() {
        MerConfig config = new MerConfig();
        config.setProductId(value("product_id", PRODUCT_ID));
        config.setSysId(value("sys_id", SYS_ID));
        config.setRsaPrivateKey(value("merchant_private_key", RSA_PRIVATE_KEY));
        config.setRsaPublicKey(value("huifu_public_key", RSA_PUBLIC_KEY));
        return config;
    }

    /** Existing DJI requests used SYS_ID here; only a configured platform value overrides it. */
    static String upperHuifuId() {
        return value("upper_huifu_id", SYS_ID);
    }

    static String value(String key, String legacyDefault) {
        String configured = TaskExecutionContext.value(key);
        if (configured == null || configured.isBlank()) {
            configured = TaskExecutionContext.value("merchant." + key);
        }
        return configured == null || configured.isBlank() ? legacyDefault : configured.trim();
    }
}
