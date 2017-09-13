Update CBD_GA_TAX_PARAM_T
set VALUE = 'N'
where GA_CODE = 'FR' and TAX_TYPE = 'POSTP' and SEQ_NO_GTP = '317';
Update CBD_GA_TAX_PARAM_T
set VALUE = 'Y'
where GA_CODE = 'FR' and TAX_TYPE = 'POSTP' and SEQ_NO_GTP = '2894';
Update CBD_TAX_PARAM_T set delete_date = null where TAX_TYPE = 'POSTP' and delete_date > sysdate-1;
delete from CBD_TAX_PARAM_T where SEQ_NO_TAXP like '555555555%';

