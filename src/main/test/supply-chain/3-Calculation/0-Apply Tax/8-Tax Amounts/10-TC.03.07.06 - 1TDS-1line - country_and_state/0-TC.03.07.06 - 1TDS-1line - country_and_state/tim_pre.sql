update pa_system_config_t set pa_system_config_t.service_configuration='both',pa_system_config_t.grouping_level='UNSPSC' ,pa_system_config_t.consolidation_treatment='parallel' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='COMPASS001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
Insert into IC_TAX_CODE_T (SEQ_NO_ITC,SEQ_NO_UNSP,TAX_CODE,CTY_CODE,STA_CODE,DEFAULT_FLAG,USERID,DELETE_DATE) values (50000020,1021,'9','CA','*','N','al.dus1',null);
Insert into PA_SKIP_TDSMERGE_T (SEQ_NO_STDSM,SEQ_NO_PARAM,GA_CODE,GA_TYPE,CLASS_1,CLASS_2,VALID_FROM,VALID_TO,USERID,DELETE_DATE) values (5000000,20,'CA','CTY',1000,1000,to_date('15-JAN-13','DD-MON-RR'),null,'tim.user1',null);
insert into IC_UNSPSC_T (SEQ_NO_UNSP,SEQ_NO_PARENT, UNSPSC_CODE, UNSPSC_BF, LEVEL_NUMBER, TITLE, DESCRIPTION, VALID_FROM, VALID_TO,userid, DELETE_DATE) values (5000009,null, '10200000', '00', 0, 'TestItem', null, to_date('01-JAN-12','DD-MON-YY'), null,'test.user', null);
insert into IC_ITEM_CLASS_T(SEQ_NO_IC,SEQ_NO_UNSP, SEQ_NO_CLASS,USERID, DELETE_DATE) VALUES (5000009,(select SEQ_NO_UNSP from IC_UNSPSC_T where UNSPSC_CODE='10200000' and TITLE='TestItem') , (select SEQ_NO_CLASS from TIM_CLASS_T  where TAX_CLASS='M' and TAX_SUB_CLASS='GOODS'),'test.user', null);
Insert into IC_TAX_CODE_T (SEQ_NO_ITC,SEQ_NO_UNSP,TAX_CODE,CTY_CODE,STA_CODE,DEFAULT_FLAG,USERID,DELETE_DATE) values (50000021,5000009,'9','CA','*','N','al.dus1',null);
update PA_TAX_CALC_RULE_T set seq_no_cr = '1006' where SEQ_NO_TI=1041 and SEQ_NO_TT='5003' and valid_to is null and VALID_FROM=to_date('01-JAN-13','DD-MON-RR');
update PA_TAX_CALC_RULE_T set seq_no_cr = '1005' where SEQ_NO_TI=1041 and SEQ_NO_TT='5002' and valid_to is null and VALID_FROM=to_date('01-JAN-13','DD-MON-RR');
