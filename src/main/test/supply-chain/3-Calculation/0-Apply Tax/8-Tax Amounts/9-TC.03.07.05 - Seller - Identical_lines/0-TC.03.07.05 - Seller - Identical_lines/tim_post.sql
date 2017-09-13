update pa_system_config_t set pa_system_config_t.service_configuration='both',pa_system_config_t.grouping_level='SUBCLASS' ,pa_system_config_t.consolidation_treatment='error' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='COMPASS001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
delete from PA_SKIP_TDSMERGE_T where SEQ_NO_STDSM>=5000000;
delete from IC_ITEM_CLASS_T where SEQ_NO_IC>=5000000;
delete from IC_TAX_CODE_T where SEQ_NO_ITC>=50000000;
delete from IC_UNSPSC_T where SEQ_NO_UNSP>=5000000;
update PA_TAX_CALC_RULE_T set seq_no_cr = '1005' where SEQ_NO_TI=1041 and SEQ_NO_TT='5003' and valid_to is null and VALID_FROM=to_date('01-JAN-13','DD-MON-RR');
update PA_TAX_CALC_RULE_T set seq_no_cr = '1001' where SEQ_NO_TI=1041 and SEQ_NO_TT='5002' and valid_to is null and VALID_FROM=to_date('01-JAN-13','DD-MON-RR');
