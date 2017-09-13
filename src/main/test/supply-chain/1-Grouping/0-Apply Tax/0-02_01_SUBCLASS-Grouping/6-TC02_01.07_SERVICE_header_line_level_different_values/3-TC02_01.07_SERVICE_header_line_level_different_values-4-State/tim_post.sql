update pa_system_config_t set service_configuration='ApplyTax', CONSOLIDATION_TREATMENT='error', TAXTRATE_TREATMENT='parallel', GROUPING_LEVEL='SUBCLASS' where seq_no_paramsrc=(SELECT seq_no_paramsrc from PARAM_SOURCE_SYSTEM_T where SEQ_NO_SRCSYS=(select seq_no_srcsys from TIM_SOURCE_SYSTEM_T where TIM_SOURCE_SYSTEM_T.ID='ICI001') AND PARAM_SOURCE_SYSTEM_T.SEQ_NO_PARAM=(select SEQ_NO_PARAM from PARAM_T where PARAM_T.NAME='SystemConfiguration'));
delete from IC_UNSPSC_T where SEQ_NO_UNSP between 5000000 and 6000000;
delete from IC_ITEM_CLASS_T where SEQ_NO_IC between 5000000 and 6000000;
DELETE FROM PA_SKIP_TDSMERGE_T where SEQ_NO_STDSM='5001' and SEQ_NO_PARAM='20' and GA_CODE='CH' and CLASS_1='1004' and CLASS_2='1004';
delete from CBD_BUSINESS_UNIT_T where SEQ_NO_BU='298045' and BU_CODE='278' and BU_TYPE='STO' and NAME='AL FUTTAIM TRADING COMPANY LLC';
