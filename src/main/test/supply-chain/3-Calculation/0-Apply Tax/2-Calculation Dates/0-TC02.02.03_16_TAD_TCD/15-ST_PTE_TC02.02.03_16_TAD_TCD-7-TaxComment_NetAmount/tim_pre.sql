update IC_TAX_CODE_T set TAX_CODE=0 where SEQ_NO_ITC=5000016;
delete from CBD_GA_TAXTYPES_T where SEQ_NO_GTT>=5000000;
update cbd_ga_taxtypes_t set valid_to=null where seq_no_gtt in (2880,2710,2711);

