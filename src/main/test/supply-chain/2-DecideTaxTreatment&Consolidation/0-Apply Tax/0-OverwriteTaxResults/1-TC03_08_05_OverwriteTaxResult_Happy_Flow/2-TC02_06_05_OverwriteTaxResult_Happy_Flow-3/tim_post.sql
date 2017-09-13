update pa_system_config_t set pa_system_config_t.consolidation_treatment='error' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='ICI001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
Delete from PA_OVERWRITE_TAXIND_T where SEQ_NO_OVRTI between 5000000 and 6000000;
update PA_TAX_CALC_RULE_T set SEQ_NO_CR='1004' where SEQ_NO_TI=1041 and SEQ_NO_TT='5001' and valid_from >=TO_DATE('01-JAN-2013','DD-MON-YYYY');
delete from IC_ITEM_CLASS_T where SEQ_NO_IC between 5000000 and 6000000;
delete from IC_TAX_CODE_T where SEQ_NO_ITC between 5000000 and 6000000;
delete from IC_UNSPSC_T where SEQ_NO_UNSP between 5000000 and 6000000;
