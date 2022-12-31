cwlVersion: v1.2
class: Workflow
requirements:
  InlineJavascriptRequirement: {}

inputs:
  overlap_files:
    default: null
    type:
      - "null"
      - type: array
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
      nested_array:
        source: overlap_files
        default: null
    run:
      class: ExpressionTool
      inputs:
        nested_array:
          type:
            - "null"
            - type: array
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
