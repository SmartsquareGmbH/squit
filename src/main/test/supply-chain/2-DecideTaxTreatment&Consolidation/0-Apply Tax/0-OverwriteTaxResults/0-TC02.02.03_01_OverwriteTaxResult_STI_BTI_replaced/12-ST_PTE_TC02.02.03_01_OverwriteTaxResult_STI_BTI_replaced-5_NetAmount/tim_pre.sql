update pa_system_config_t set pa_system_config_t.consolidation_treatment='parallel' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='ICI001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
Insert into PA_OVERWRITE_TAXIND_T (SEQ_NO_OVRTI,SEQ_NO_PARAM,GA_CODE,GA_TYPE,BU_CODE,BU_TYPE,TAXIND,SELLER_FLAG,BUYER_FLAG,OVERWRITE_STI,OVERWRITE_BTI,VALID_FROM,VALID_TO,USERID,DELETE_DATE) values (5000031,19,'CAON','STA','3101','COM',1041,'N','Y',null,1066,to_date('01-SEP-11','DD-MON-RR'),null,'tim.user1',null);
