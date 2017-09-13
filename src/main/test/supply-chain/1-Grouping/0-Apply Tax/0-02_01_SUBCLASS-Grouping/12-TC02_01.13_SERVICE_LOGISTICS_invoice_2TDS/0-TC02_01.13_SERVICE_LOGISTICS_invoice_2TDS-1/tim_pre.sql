update pa_system_config_t set pa_system_config_t.consolidation_treatment='parallel' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='ICI001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
Insert into CBD_BUSINESS_UNIT_T (SEQ_NO_BU,BU_CODE,BU_TYPE,NAME,VALID_FROM,VALID_TO,IDRS_IMPORT_DATE,DELETE_DATE) values (298045,'278','STO','AL FUTTAIM TRADING COMPANY LLC',to_date('25-JUL-12','DD-MON-RR'),null,to_date('10-APR-13','DD-MON-RR'),null);
insert into IC_UNSPSC_T (SEQ_NO_UNSP,SEQ_NO_PARENT, UNSPSC_CODE, UNSPSC_BF, LEVEL_NUMBER, TITLE, DESCRIPTION, VALID_FROM, VALID_TO,userid, DELETE_DATE) values (5000014,null, '50102000', '00', 0, 'TestItem', null, to_date('01-JAN-12','DD-MON-YY'), null,'test.user', null);
insert into IC_ITEM_CLASS_T(SEQ_NO_IC,SEQ_NO_UNSP, SEQ_NO_CLASS,USERID, DELETE_DATE) VALUES (5000014,(select SEQ_NO_UNSP from IC_UNSPSC_T where UNSPSC_CODE='50102000' and TITLE='TestItem') , (select SEQ_NO_CLASS from TIM_CLASS_T  where TAX_CLASS='S' and TAX_SUB_CLASS='WARRANTY'),'test.user', null);
insert into IC_UNSPSC_T (SEQ_NO_UNSP,SEQ_NO_PARENT, UNSPSC_CODE, UNSPSC_BF, LEVEL_NUMBER, TITLE, DESCRIPTION, VALID_FROM, VALID_TO,userid, DELETE_DATE) values (5000013,null, '30000000', '00', 0, 'TestItem', null, to_date('01-JAN-12','DD-MON-YY'), null,'test.user', null);
insert into IC_ITEM_CLASS_T(SEQ_NO_IC,SEQ_NO_UNSP, SEQ_NO_CLASS,USERID, DELETE_DATE) VALUES (5000013,(select SEQ_NO_UNSP from IC_UNSPSC_T where UNSPSC_CODE='30000000' and TITLE='TestItem') , (select SEQ_NO_CLASS from TIM_CLASS_T  where TAX_CLASS='S' and TAX_SUB_CLASS='LOGISTICS'),'test.user', null);
INSERT INTO PA_SKIP_TDSMERGE_T(SEQ_NO_STDSM, SEQ_NO_PARAM, GA_CODE, GA_TYPE, CLASS_1, CLASS_2, VALID_FROM, VALID_TO, USERID, DELETE_DATE ) VALUES (5000,'20','CZ','CTY','1002','1009', to_date('25-JUL-12','DD-MON-RR'),null,'test.user',null);