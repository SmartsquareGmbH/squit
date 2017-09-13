Update CBD_TAX_PARAM_T set delete_date = null where TAX_TYPE = 'POSTP' and delete_date > sysdate-1;
delete from CBD_TAX_PARAM_T where SEQ_NO_TAXP like '555555555%';

