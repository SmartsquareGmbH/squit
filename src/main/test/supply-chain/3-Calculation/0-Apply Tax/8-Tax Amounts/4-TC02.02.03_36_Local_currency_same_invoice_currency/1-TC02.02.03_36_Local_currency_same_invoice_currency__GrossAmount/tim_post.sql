update PA_SYSTEM_CONFIG_T SET SERVICE_CONFIGURATION='both', GROUPING_LEVEL='SUBCLASS', CONSOLIDATION_TREATMENT='error', TAXTRATE_TREATMENT='error' WHERE SEQ_NO_PARAMSRC=(select PA_SYSTEM_CONFIG_T.seq_no_paramsrc from PARAM_T,PARAM_SOURCE_SYSTEM_T,PA_SYSTEM_CONFIG_T,TIM_SOURCE_SYSTEM_T where PARAM_T.seq_no_param=PARAM_SOURCE_SYSTEM_T.seq_no_param and PARAM_T.name='SystemConfiguration'and TIM_SOURCE_SYSTEM_T.seq_no_srcsys=PARAM_SOURCE_SYSTEM_T.seq_no_srcsys and TIM_SOURCE_SYSTEM_T.ID='COMPASS001' and PA_SYSTEM_CONFIG_T.seq_no_paramsrc=PARAM_SOURCE_SYSTEM_T.seq_no_paramsrc and PARAM_SOURCE_SYSTEM_T.valid_to is null and PARAM_SOURCE_SYSTEM_T.delete_date is null);
update PA_TAX_CALC_RULE_T set seq_no_cr=1002 where seq_no_ti=1065 and SEQ_NO_TT=5001 and valid_from=TO_DATE('01-SEP-11', 'DD-MON-RR');