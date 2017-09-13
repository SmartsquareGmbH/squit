delete from CBD_GA_TAXTYPES_T where SEQ_NO_GTT>=5000000;
update cbd_ga_taxtypes_t set valid_to=null where seq_no_gtt in (2880,2710,2711);
