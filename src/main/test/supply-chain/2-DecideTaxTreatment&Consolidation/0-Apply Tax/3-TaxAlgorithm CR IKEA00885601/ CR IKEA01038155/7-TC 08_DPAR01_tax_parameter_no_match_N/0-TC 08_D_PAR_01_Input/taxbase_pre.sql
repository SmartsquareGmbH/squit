Update CBD_GA_TAX_PARAM_T
set DELETE_DATE = SYSDATE
where GA_CODE = 'FR' and TAX_TYPE = 'POSTP' and SEQ_NO_GTP = '317';
Update CBD_GA_TAX_PARAM_T
set DELETE_DATE = SYSDATE
where GA_CODE = 'FR' and TAX_TYPE = 'POSTP' and SEQ_NO_GTP = '2894';
Update CBD_TAX_PARAM_T set delete_date = sysdate where TAX_TYPE = 'POSTP';
