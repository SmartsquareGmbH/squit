update cbd_tax_rate_t set valid_to=null where seq_no_tr='474';
delete from CBD_TAX_RATE_T where SEQ_NO_TR between 5000000 and 6000000;
delete from CBD_GA_TAXTYPES_T where SEQ_NO_GTT between 5000000 and 6000000;
update cbd_ga_taxtypes_t set valid_to=null where seq_no_gtt in (2880,2710,2711);
update CBD_GEOGRAPHICAL_AREA_T set valid_to=null where seq_no_ga=48732;
delete CBD_GEOGRAPHICAL_AREA_T where seq_no_ga between 5000000 and 6000000;
update cbd_ga_taxtypes_t set valid_to=null where seq_no_gtt=2889;
