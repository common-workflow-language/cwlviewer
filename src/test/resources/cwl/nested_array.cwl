cwlVersion: v1.2
class: Workflow

inputs:
  overlap_files:
    type:
      type: array
      items:
        type: array
        items: File

outputs:
  freq_files:
    type:
      type: array
      items:
        type: array
        items: File
    outputSource: dummy/freq_files

steps:
  dummy:
    in:
      nested_array: overlap_files
    run:
      class: ExpressionTool
      inputs:
        nested_array:
          type:
            type: array
            items:
              type: array
              items: File
      expression: |
        ${ return {"freq_files": inputs.nested_array }; }
      outputs:
        freq_files:
          type:
            type: array
            items:
              type: array
              items: File
    out: [ freq_files ]
