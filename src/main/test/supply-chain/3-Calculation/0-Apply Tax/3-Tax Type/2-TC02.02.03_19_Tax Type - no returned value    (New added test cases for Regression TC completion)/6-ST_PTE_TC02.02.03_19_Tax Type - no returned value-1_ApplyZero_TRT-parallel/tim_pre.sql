update pa_tax_calc_rule_t set pa_tax_calc_rule_t.seq_no_cr='1002' where pa_tax_calc_rule_t.seq_no_tt is NULL and pa_tax_calc_rule_t.seq_no_ti= (select pa_tax_indicator_t.seq_no_ti from pa_tax_indicator_t where pa_tax_indicator_t.taxind='MAT_NOFI');
