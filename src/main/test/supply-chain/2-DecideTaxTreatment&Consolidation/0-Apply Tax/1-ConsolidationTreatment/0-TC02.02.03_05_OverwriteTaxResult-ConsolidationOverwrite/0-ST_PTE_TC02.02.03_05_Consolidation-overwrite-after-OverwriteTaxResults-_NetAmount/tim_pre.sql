update pa_system_config_t set pa_system_config_t.consolidation_treatment='overwrite' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='ICI001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
Insert into PA_OVERWRITE_TAXIND_T (SEQ_NO_OVRTI,SEQ_NO_PARAM,GA_CODE,GA_TYPE,BU_CODE,BU_TYPE,TAXIND,SELLER_FLAG,BUYER_FLAG,OVERWRITE_STI,OVERWRITE_BTI,VALID_FROM,VALID_TO,USERID,DELETE_DATE) values (5000001,19,'CH','CTY','5100','COM',1068,'Y','N',null,1041,to_date('01-SEP-11','DD-MON-RR'),null,'tim.user1',null);
