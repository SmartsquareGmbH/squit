Insert into PA_SKIP_TDSMERGE_T (SEQ_NO_STDSM,SEQ_NO_PARAM,GA_CODE,GA_TYPE,CLASS_1,CLASS_2,VALID_FROM,VALID_TO,USERID,DELETE_DATE) values (5000005,20,'CA','CTY', (select SEQ_NO_CLASS from TIM_CLASS_T where TAX_SUB_CLASS='HANDLING' and valid_to is null), (select SEQ_NO_CLASS from TIM_CLASS_T where TAX_SUB_CLASS='HANDLING' and valid_to is null),to_date('09-MAY-12','DD-MON-RR'),null,'al.dus1',null);
commit;
