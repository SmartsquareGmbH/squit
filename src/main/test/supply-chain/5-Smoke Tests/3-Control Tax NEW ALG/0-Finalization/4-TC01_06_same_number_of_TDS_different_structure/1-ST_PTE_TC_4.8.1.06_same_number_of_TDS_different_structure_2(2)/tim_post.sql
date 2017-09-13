update pa_system_config_t set service_configuration='ApplyTax' where seq_no_paramsrc = (select seq_no_paramsrc from param_source_system_t where seq_no_param=(select seq_no_param from param_t where name='SystemConfiguration') and seq_no_srcsys = (select seq_no_srcsys from tim_source_system_t where id='FRIM001'));
delete from pa_matching_level_t where seq_no_ml >= 5000000 and seq_no_ml <= 6000000;
update pa_system_config_t set consolidation_treatment='error' where seq_no_paramsrc = (select seq_no_paramsrc from param_source_system_t where seq_no_param=(select seq_no_param from param_t where name='SystemConfiguration') and seq_no_srcsys = (select seq_no_srcsys from tim_source_system_t where id='FRIM001'));
delete from PARAM_SOURCE_SYSTEM_T where seq_no_paramsrc >= 5000000 and seq_no_paramsrc < 6000000;
update pa_system_config_t set service_configuration='ApplyTax' where seq_no_paramsrc = (select seq_no_paramsrc from param_source_system_t where seq_no_param=(select seq_no_param from param_t where name='SystemConfiguration') and seq_no_srcsys = (select seq_no_srcsys from tim_source_system_t where id='ICI001'));
update pa_system_config_t set consolidation_treatment='error' where seq_no_paramsrc = (select seq_no_paramsrc from param_source_system_t where seq_no_param=(select seq_no_param from param_t where name='SystemConfiguration') and seq_no_srcsys = (select seq_no_srcsys from tim_source_system_t where id='ICI001'));
delete from IC_TAX_CODE_T where SEQ_NO_ITC between 5000000 and 6000000;
update pa_system_config_t set consolidation_treatment='error', taxtrate_treatment='error' where seq_no_paramsrc = (select seq_no_paramsrc from param_source_system_t where seq_no_param=(select seq_no_param from param_t where name='SystemConfiguration') and seq_no_srcsys = (select seq_no_srcsys from tim_source_system_t where id='COMPASS001'));
update param_source_system_t set valid_from=to_date('01-SEP-11','DD-MON-RR') where seq_no_paramsrc='1103';
