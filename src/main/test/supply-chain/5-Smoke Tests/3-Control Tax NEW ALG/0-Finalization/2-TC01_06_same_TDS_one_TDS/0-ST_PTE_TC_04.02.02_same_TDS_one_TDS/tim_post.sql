update PA_TAX_CALC_RULE_T set SEQ_NO_CR=1002  where SEQ_NO_CR=1001 and seq_no_tt=5001 and SEQ_NO_TI=1039;
delete from IC_TAX_CODE_T where SEQ_NO_ITC between 5000000 and 6000000;
