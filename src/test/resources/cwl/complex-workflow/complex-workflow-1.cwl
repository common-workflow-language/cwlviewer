# Source: https://github.com/heliumdatacommons/TOPMed_RNAseq_CWL/tree/578066ec5d6847892528f973b22531d4c8487280/workflows/complex-workflow

# BSD 3-Clause License
# 
# Copyright (c) 2018, RTI International
# All rights reserved.
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
# 
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# 
# * Redistributions in binary form must reproduce the above copyright notice,
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# 
# * Neither the name of the copyright holder nor the names of its
#   contributors may be used to endorse or promote products derived from
#   this software without specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

cwlVersion: v1.0
class: Workflow
id: complex-workflow
inputs:
  archive:
    type: string
  files:
    type:
      type: array
      items: File
  new_archive:
    type: string
  touch_files:
    type:
      type: array
      items: string
  echo_message:
    type: string
  echo_output_location:
    type: string

outputs:
  archive_out:
    type: File
    outputSource: tar_step/archive_out
  touch_out:
    type:
      type: array
      items: File
    outputSource: touch_step/file_out
  echo_out:
    type: File
    outputSource: echo_step/echo_output
  re_tar_out:
    type: File
    outputSource: re_tar_step/archive_out

steps:
  tar_step:
    run: tar.cwl
    in:
      archive_file:
        source: "#archive"
      file_list:
        source: "#files"
    out: [archive_out]

  touch_step:
    run: touch.cwl
    scatter: filename
    scatterMethod: dotproduct
    in:
      waitfor:
        source: "#tar_step/archive_out"
      filename:
        source: "#touch_files"
    out: [file_out]

  echo_step:
    run: echo.cwl
    in:
      message: "#echo_message"
      output_location: "#echo_output_location"
    out: [echo_output]

  re_tar_step:
    run: tar.cwl
    in:
      archive_file:
        source: "#archive"
      file_list:
        source: ["#touch_step/file_out", "#files"]
        linkMerge: merge_flattened
    out: [archive_out]

  # rm_step:
  #   run: rm.cwl
  #   scatter: filename
  #   scatterMethod: dotproduct
  #   in:
  #     waitfor:
  #       source: "#re_tar_step/archive_out"
  #     filename:
  #       source: ["#files", "#touch_step/file_out", "#echo_step/echo_output"]
  #       linkMerge: merge_flattened
  #   out: [standard_out]

requirements:
  - class: StepInputExpressionRequirement
  - class: ScatterFeatureRequirement
  - class: MultipleInputFeatureRequirement
