delete from IC_ITEM_CLASS_T where SEQ_NO_IC>=5000000;
delete from IC_TAX_CODE_T where SEQ_NO_ITC>=5000000;
delete from IC_UNSPSC_T where SEQ_NO_UNSP>=5000000;
update pa_system_config_t set pa_system_config_t.taxtrate_treatment='parallel' where pa_system_config_t.seq_no_paramsrc=( select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration' and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='ICI001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);


