cwlVersion: v1.0
class: Workflow

inputs:
  ncrna_tab_file: {type: File?}
  reverse_reads: File?
  qualified_phred_quality: { type: int? }
  
steps: []

outputs: []