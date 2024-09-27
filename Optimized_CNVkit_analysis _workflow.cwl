#!/usr/bin/env cwl-runner  
  
cwlVersion: v1.0  
class: Workflow  
label: "Optimized CNVkit Analysis Workflow"  
  
requirements:  
  - class: SubworkflowFeatureRequirement  
  - class: StepInputExpressionRequirement  
  - class: MultipleInputFeatureRequirement  
  - class: ScatterFeatureRequirement  
  
inputs:  
  bams:  
    type: File[]  
    secondaryFiles: [.bai]  
  targets:  
    type: File  
  anti_targets:  
    type: File  
  reference_coverage:  
    type: File  
  
outputs:  
  target_coverage:  
    type: File[]  
    outputSource: target_sample_coverage/coverage  
  antitarget_coverage:  
    type: File[]  
    outputSource: antitarget_sample_coverage/coverage  
  fixed_cnr:  
    type: File[]  
    outputSource: fix_coverage/ratios  
  segmented:  
    type: File[]  
    outputSource: cna_segments/cna_segmented  
  
steps:  
  target_sample_coverage:  
    run: ../tools/cnvkit_coverage.cwl  
    scatter: [bam]  
    in:  
      bam: bams  
      bed: targets  
      istarget: true  
    out: [coverage]  
  
  antitarget_sample_coverage:  
    run: ../tools/cnvkit_coverage.cwl  
    scatter: [bam]  
    in:  
      bam: bams  
      bed: anti_targets  
      istarget: false  
    out: [coverage]  
  
  # Assuming a custom step or tool to handle both target and antitarget coverage  
  # This step might need to be adjusted based on the actual tool or method  
  fix_coverage:  
    run: ../tools/custom_fix_coverage.cwl  # Placeholder for a custom tool  
    in:  
      target_coverage: target_sample_coverage/coverage  
      antitarget_coverage: antitarget_sample_coverage/coverage  
      reference_coverage: reference_coverage  
    out: [ratios]  
  
  cna_segments:  
    run: ../tools/cnvkit_segment.cwl  
    in:  
      coverage: fix_coverage/ratios  
    out: [cna_segmented]
