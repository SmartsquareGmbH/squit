update pa_system_config_t set pa_system_config_t.TAXTRATE_TREATMENT='parallel' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='COMPASS001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
insert into IC_UNSPSC_T (SEQ_NO_UNSP,SEQ_NO_PARENT, UNSPSC_CODE, UNSPSC_BF, LEVEL_NUMBER, TITLE, DESCRIPTION, VALID_FROM, VALID_TO,userid, DELETE_DATE)  values (5000016,null, '10200000', '00', 0, 'TestItem 1', null, to_date('20-DEC-12','DD-MON-YY'), null,'test.user', null);
insert into IC_ITEM_CLASS_T(SEQ_NO_IC,SEQ_NO_UNSP, SEQ_NO_CLASS,USERID, DELETE_DATE)  VALUES (5000016,(select SEQ_NO_UNSP from IC_UNSPSC_T where UNSPSC_CODE='10200000' and TITLE='TestItem 1') , (select SEQ_NO_CLASS from TIM_CLASS_T  where TAX_CLASS='M' and TAX_SUB_CLASS='GOODS'),'test.user', null);
Insert into IC_TAX_CODE_T (SEQ_NO_ITC,SEQ_NO_UNSP,TAX_CODE,CTY_CODE,STA_CODE,DEFAULT_FLAG,USERID,DELETE_DATE) values (5000016,5000016,'9','CA','*','N','al.dus1',null);
