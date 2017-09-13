update pa_system_config_t set service_configuration='ApplyTax', CONSOLIDATION_TREATMENT='error', TAXTRATE_TREATMENT='parallel', GROUPING_LEVEL='SUBCLASS' where seq_no_paramsrc=(SELECT seq_no_paramsrc from PARAM_SOURCE_SYSTEM_T where SEQ_NO_SRCSYS=(select seq_no_srcsys from TIM_SOURCE_SYSTEM_T where TIM_SOURCE_SYSTEM_T.ID='ICI001') AND PARAM_SOURCE_SYSTEM_T.SEQ_NO_PARAM=(select SEQ_NO_PARAM from PARAM_T where PARAM_T.NAME='SystemConfiguration'));
delete from CBD_BUSINESS_UNIT_T where SEQ_NO_BU='298045' and BU_CODE='278' and BU_TYPE='STO' and NAME='AL FUTTAIM TRADING COMPANY LLC';
delete from CBD_BUSINESS_UNIT_T where SEQ_NO_BU='298050' and BU_CODE='118' and BU_TYPE='STO' and NAME='IKEA CHEMNITZ';
delete from CBD_BUSINESS_UNIT_T where SEQ_NO_BU='298048' and BU_CODE='150' and BU_TYPE='STO' and NAME='IKEA COVENTRY';
DELETE FROM PA_SKIP_TDSMERGE_T where SEQ_NO_STDSM >= 5000;
