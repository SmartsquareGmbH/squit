update pa_system_config_t set service_configuration='both', CONSOLIDATION_TREATMENT='parallel', TAXTRATE_TREATMENT='parallel', GROUPING_LEVEL='SUBCLASS' where seq_no_paramsrc=(SELECT seq_no_paramsrc from PARAM_SOURCE_SYSTEM_T where SEQ_NO_SRCSYS=(select seq_no_srcsys from TIM_SOURCE_SYSTEM_T where TIM_SOURCE_SYSTEM_T.ID='ICI001') AND PARAM_SOURCE_SYSTEM_T.SEQ_NO_PARAM=(select SEQ_NO_PARAM from PARAM_T where PARAM_T.NAME='SystemConfiguration'));
insert into IC_UNSPSC_T (SEQ_NO_UNSP,SEQ_NO_PARENT, UNSPSC_CODE, UNSPSC_BF, LEVEL_NUMBER, TITLE, DESCRIPTION, VALID_FROM, VALID_TO,userid, DELETE_DATE)   values (5000013,null, '30000000', '00', 0, 'TestItem', null, to_date('01-JAN-12','DD-MON-YY'), null,'test.user', null);
insert into IC_ITEM_CLASS_T(SEQ_NO_IC,SEQ_NO_UNSP, SEQ_NO_CLASS,USERID, DELETE_DATE) VALUES (5000013,(select SEQ_NO_UNSP from IC_UNSPSC_T where UNSPSC_CODE='30000000' and TITLE='TestItem') , (select SEQ_NO_CLASS from TIM_CLASS_T  where TAX_CLASS='S' and TAX_SUB_CLASS='LOGISTICS'),'test.user', null);
insert into IC_UNSPSC_T (SEQ_NO_UNSP,SEQ_NO_PARENT, UNSPSC_CODE, UNSPSC_BF, LEVEL_NUMBER, TITLE, DESCRIPTION, VALID_FROM, VALID_TO,userid, DELETE_DATE)   values (5000014,null, '81141601', '00', 0, 'TestItem', null, to_date('01-JAN-12','DD-MON-YY'), null,'test.user', null);
insert into IC_ITEM_CLASS_T(SEQ_NO_IC,SEQ_NO_UNSP, SEQ_NO_CLASS,USERID, DELETE_DATE) VALUES (5000014,(select SEQ_NO_UNSP from IC_UNSPSC_T where UNSPSC_CODE='81141601' and TITLE='TestItem') , (select SEQ_NO_CLASS from TIM_CLASS_T  where TAX_CLASS='S' and TAX_SUB_CLASS='LOGISTICS'),'test.user', null);
INSERT INTO PA_SKIP_TDSMERGE_T(SEQ_NO_STDSM, SEQ_NO_PARAM, GA_CODE, GA_TYPE, CLASS_1, CLASS_2, VALID_FROM, VALID_TO, USERID, DELETE_DATE ) VALUES (5001,'20','CH','CTY','1002','1002', to_date('25-JUL-12','DD-MON-RR'),null,'test.user',null);
Insert into CBD_BUSINESS_UNIT_T (SEQ_NO_BU,BU_CODE,BU_TYPE,NAME,VALID_FROM,VALID_TO,IDRS_IMPORT_DATE,DELETE_DATE) values (298045,'278','STO','AL FUTTAIM TRADING COMPANY LLC',to_date('25-JUL-12','DD-MON-RR'),null,to_date('10-APR-13','DD-MON-RR'),null);