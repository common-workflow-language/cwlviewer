#!/usr/bin/env cwl-runner

cwlVersion: v1.0
class: CommandLineTool
label: "CNVkit coverage"

baseCommand: ["cnvkit.py", "coverage"]

requirements:
    - class: ResourceRequirement
      coresMin: 4
      ramMin: 64000
    - class: DockerRequirement
      dockerPull: "jbwebster/cna_pipeline_docker"

inputs:
    bam:
        type: File
        secondaryFiles: [.bai]
        inputBinding:
            position: 1
    bed:
        type: File
        inputBinding:
            position: 2
    istarget:
        type: boolean

arguments:
 - valueFrom: |
    ${
      var x = String(inputs.bam.nameroot)
      var suffix = "";
      if (inputs.istarget) {
        suffix = ".targetcoverage.cnn";
      } else {
        suffix = ".antitargetcoverage.cnn";
      }
      var xx = x + suffix;
      return xx;
    }
   position: 3
   prefix: "-o"

outputs:
 coverage:
  type: File
  outputBinding: 
   glob: "*coverage.cnn"
