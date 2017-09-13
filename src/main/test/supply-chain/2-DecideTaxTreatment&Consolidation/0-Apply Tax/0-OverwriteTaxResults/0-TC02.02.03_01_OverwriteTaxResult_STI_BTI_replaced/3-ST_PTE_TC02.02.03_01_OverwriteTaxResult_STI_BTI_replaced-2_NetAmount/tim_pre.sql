update pa_system_config_t set pa_system_config_t.consolidation_treatment='parallel' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='ICI001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
Insert into PA_OVERWRITE_TAXIND_T (SEQ_NO_OVRTI,SEQ_NO_PARAM,GA_CODE,GA_TYPE,BU_CODE,BU_TYPE,TAXIND,SELLER_FLAG,BUYER_FLAG,OVERWRITE_STI,OVERWRITE_BTI,VALID_FROM,VALID_TO,USERID,DELETE_DATE) values (5000000,19,'CA','CTY','5100','COM',1039,'Y','N',null,1037,to_date('01-SEP-11','DD-MON-RR'),null,'tim.user1',null);
Insert into PA_OVERWRITE_TAXIND_T (SEQ_NO_OVRTI,SEQ_NO_PARAM,GA_CODE,GA_TYPE,BU_CODE,BU_TYPE,TAXIND,SELLER_FLAG,BUYER_FLAG,OVERWRITE_STI,OVERWRITE_BTI,VALID_FROM,VALID_TO,USERID,DELETE_DATE) values (5000001,19,'CA','CTY','7520','COM',1063,'N','Y',null,1037,to_date('01-SEP-11','DD-MON-RR'),null,'tim.user1',null);
update PA_TAX_CALC_RULE_T set SEQ_NO_CR='1001' where SEQ_NO_TI=1039 and SEQ_NO_TT='5002' and valid_from <=TO_DATE('28-OCT-2013','DD-MON-RR');
update PA_TAX_CALC_RULE_T set SEQ_NO_CR='1001' where SEQ_NO_TI=1037 and SEQ_NO_TT='5001' and valid_from <=TO_DATE('28-OCT-2013','DD-MON-YYYY');