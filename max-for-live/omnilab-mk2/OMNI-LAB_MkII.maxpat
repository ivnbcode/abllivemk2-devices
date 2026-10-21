{
  "patcher": {
    "fileversion": 1,
    "appversion": {
      "major": 8,
      "minor": 5,
      "revision": 6,
      "architecture": "x64",
      "modernui": 1
    },
    "classnamespace": "box",
    "rect": [100.0, 100.0, 920.0, 560.0],
    "bglocked": 0,
    "openinpresentation": 1,
    "default_fontsize": 10.0,
    "default_fontname": "Arial Bold",
    "gridonopen": 1,
    "gridsize": [15.0, 15.0],
    "gridsnaponopen": 1,
    "objectsnaponopen": 1,
    "statusbarvisible": 2,
    "toolbarvisible": 1,
    "boxes": [
      {
        "box": {
          "id": "obj-panel-bg",
          "maxclass": "panel",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [15.0, 15.0, 580.0, 170.0],
          "presentation": 1,
          "presentation_rect": [0.0, 0.0, 540.0, 170.0],
          "bgcolor": [0.14, 0.13, 0.12, 1.0],
          "rounded": 8
        }
      },
      {
        "box": {
          "id": "obj-title",
          "maxclass": "comment",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [30.0, 20.0, 260.0, 20.0],
          "presentation": 1,
          "presentation_rect": [12.0, 8.0, 280.0, 20.0],
          "text": "OMNI-LAB MkII — SONIC STRINGS (ARTURIA CC1)",
          "textcolor": [0.95, 0.72, 0.25, 1.0],
          "fontsize": 11.0
        }
      },
      {
        "box": {
          "id": "obj-chord-display",
          "maxclass": "comment",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [360.0, 310.0, 160.0, 36.0],
          "presentation": 1,
          "presentation_rect": [14.0, 32.0, 150.0, 38.0],
          "text": "C Major",
          "textcolor": [0.28, 0.96, 0.78, 1.0],
          "fontsize": 22.0
        }
      },
      {
        "box": {
          "id": "obj-notes-display",
          "maxclass": "comment",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [360.0, 355.0, 160.0, 20.0],
          "presentation": 1,
          "presentation_rect": [14.0, 68.0, 150.0, 18.0],
          "text": "C · E · G",
          "textcolor": [0.85, 0.85, 0.85, 1.0],
          "fontsize": 10.0
        }
      },
      {
        "box": {
          "id": "obj-notein",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 3,
          "outlettype": ["int", "int", "int"],
          "patching_rect": [40.0, 60.0, 46.0, 20.0],
          "text": "notein"
        }
      },
      {
        "box": {
          "id": "obj-pack-note",
          "maxclass": "newobj",
          "numinlets": 2,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [40.0, 95.0, 56.0, 20.0],
          "text": "pack 0 0"
        }
      },
      {
        "box": {
          "id": "obj-prepend-note",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [40.0, 130.0, 78.0, 20.0],
          "text": "prepend note"
        }
      },
      {
        "box": {
          "id": "obj-ctlin1",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["int", "int"],
          "patching_rect": [145.0, 60.0, 46.0, 20.0],
          "text": "ctlin 1"
        }
      },
      {
        "box": {
          "id": "obj-prepend-cc1",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [145.0, 130.0, 72.0, 20.0],
          "text": "prepend cc1"
        }
      },
      {
        "box": {
          "id": "obj-dial-strum",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [240.0, 60.0, 55.0, 48.0],
          "presentation": 1,
          "presentation_rect": [180.0, 32.0, 60.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "Strum Position (CC1)",
              "parameter_shortname": "Strum",
              "parameter_type": 0,
              "parameter_mmin": 0.0,
              "parameter_mmax": 1.0,
              "parameter_unitstyle": 1
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prepend-strum",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [240.0, 130.0, 82.0, 20.0],
          "text": "prepend strum"
        }
      },
      {
        "box": {
          "id": "obj-dial-root",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [340.0, 60.0, 50.0, 48.0],
          "presentation": 1,
          "presentation_rect": [250.0, 32.0, 55.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "Chord Root",
              "parameter_shortname": "Root",
              "parameter_type": 2,
              "parameter_enum": ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prepend-root",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [340.0, 130.0, 74.0, 20.0],
          "text": "prepend root"
        }
      },
      {
        "box": {
          "id": "obj-dial-quality",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [430.0, 60.0, 55.0, 48.0],
          "presentation": 1,
          "presentation_rect": [315.0, 32.0, 60.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "Chord Quality",
              "parameter_shortname": "Quality",
              "parameter_type": 2,
              "parameter_enum": ["Major", "Minor", "7th", "Maj7", "Min7", "Dim", "Aug", "Sus4"]
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prepend-quality",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [430.0, 130.0, 88.0, 20.0],
          "text": "prepend quality"
        }
      },
      {
        "box": {
          "id": "obj-dial-sustain",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [530.0, 60.0, 55.0, 48.0],
          "presentation": 1,
          "presentation_rect": [385.0, 32.0, 60.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "Sonic Strings Sustain",
              "parameter_shortname": "Sustain",
              "parameter_type": 0,
              "parameter_mmin": 80.0,
              "parameter_mmax": 3500.0,
              "parameter_initial_enable": 1,
              "parameter_initial": [1200.0],
              "parameter_unitstyle": 2
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-toggle-hold",
          "maxclass": "live.toggle",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "parameter_enable": 1,
          "patching_rect": [610.0, 65.0, 24.0, 24.0],
          "presentation": 1,
          "presentation_rect": [460.0, 42.0, 24.0, 24.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "Chord Hold Organ",
              "parameter_shortname": "Hold",
              "parameter_type": 2,
              "parameter_enum": ["Off", "On"],
              "parameter_initial_enable": 1,
              "parameter_initial": [1]
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prepend-hold",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [610.0, 130.0, 108.0, 20.0],
          "text": "prepend chord_hold"
        }
      },
      {
        "box": {
          "id": "obj-js-core",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 3,
          "outlettype": ["", "", ""],
          "patching_rect": [145.0, 210.0, 145.0, 20.0],
          "saved_object_attributes": {
            "filename": "omnilab_m4l.js",
            "parameter_enable": 0
          },
          "text": "js omnilab_m4l.js"
        }
      },
      {
        "box": {
          "id": "obj-unpack-strum",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["int", "int"],
          "patching_rect": [80.0, 265.0, 68.0, 20.0],
          "text": "unpack 0 0"
        }
      },
      {
        "box": {
          "id": "obj-makenote",
          "maxclass": "newobj",
          "numinlets": 3,
          "numoutlets": 2,
          "outlettype": ["float", "float"],
          "patching_rect": [80.0, 310.0, 110.0, 20.0],
          "text": "makenote 95 1200"
        }
      },
      {
        "box": {
          "id": "obj-noteout-strum",
          "maxclass": "newobj",
          "numinlets": 3,
          "numoutlets": 0,
          "patching_rect": [80.0, 365.0, 56.0, 20.0],
          "text": "noteout"
        }
      },
      {
        "box": {
          "id": "obj-unpack-hold",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["int", "int"],
          "patching_rect": [210.0, 265.0, 68.0, 20.0],
          "text": "unpack 0 0"
        }
      },
      {
        "box": {
          "id": "obj-route-ui",
          "maxclass": "newobj",
          "numinlets": 3,
          "numoutlets": 3,
          "outlettype": ["", "", ""],
          "patching_rect": [360.0, 245.0, 195.0, 20.0],
          "text": "route chord_fullname chord_notes"
        }
      },
      {
        "box": {
          "id": "obj-set-chord-text",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [360.0, 280.0, 72.0, 20.0],
          "text": "prepend set"
        }
      },
      {
        "box": {
          "id": "obj-set-notes-text",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [450.0, 280.0, 72.0, 20.0],
          "text": "prepend set"
        }
      },
      {
        "box": {
          "id": "obj-help-comment",
          "maxclass": "comment",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [30.0, 420.0, 510.0, 40.0],
          "presentation": 1,
          "presentation_rect": [14.0, 102.0, 510.0, 52.0],
          "text": "• Tira Modulación (CC 1) del Arturia MiniLab MkII = Rasguea las 16 Sonic Strings (4 octavas)\n• Notas 36–43 (C1–G1 / Pads 1–8) = Selección rápida de 8 acordes predefinidos\n• Teclas C2+ (48+) = Detección de acordes en vivo (1 tecla = Raíz, Tríada/7ma = Acorde completo)",
          "textcolor": [0.75, 0.72, 0.68, 1.0],
          "fontsize": 9.5
        }
      }
    ],
    "lines": [
      {
        "patchline": {
          "source": ["obj-notein", 0],
          "destination": ["obj-pack-note", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-notein", 1],
          "destination": ["obj-pack-note", 1]
        }
      },
      {
        "patchline": {
          "source": ["obj-pack-note", 0],
          "destination": ["obj-prepend-note", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-prepend-note", 0],
          "destination": ["obj-js-core", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-ctlin1", 0],
          "destination": ["obj-prepend-cc1", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-prepend-cc1", 0],
          "destination": ["obj-js-core", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-dial-strum", 0],
          "destination": ["obj-prepend-strum", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-prepend-strum", 0],
          "destination": ["obj-js-core", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-dial-root", 0],
          "destination": ["obj-prepend-root", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-prepend-root", 0],
          "destination": ["obj-js-core", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-dial-quality", 0],
          "destination": ["obj-prepend-quality", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-prepend-quality", 0],
          "destination": ["obj-js-core", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-dial-sustain", 0],
          "destination": ["obj-makenote", 2]
        }
      },
      {
        "patchline": {
          "source": ["obj-toggle-hold", 0],
          "destination": ["obj-prepend-hold", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-prepend-hold", 0],
          "destination": ["obj-js-core", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-js-core", 0],
          "destination": ["obj-unpack-strum", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-unpack-strum", 0],
          "destination": ["obj-makenote", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-unpack-strum", 1],
          "destination": ["obj-makenote", 1]
        }
      },
      {
        "patchline": {
          "source": ["obj-makenote", 0],
          "destination": ["obj-noteout-strum", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-makenote", 1],
          "destination": ["obj-noteout-strum", 1]
        }
      },
      {
        "patchline": {
          "source": ["obj-js-core", 1],
          "destination": ["obj-unpack-hold", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-unpack-hold", 0],
          "destination": ["obj-noteout-strum", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-unpack-hold", 1],
          "destination": ["obj-noteout-strum", 1]
        }
      },
      {
        "patchline": {
          "source": ["obj-js-core", 2],
          "destination": ["obj-route-ui", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-route-ui", 0],
          "destination": ["obj-set-chord-text", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-set-chord-text", 0],
          "destination": ["obj-chord-display", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-route-ui", 1],
          "destination": ["obj-set-notes-text", 0]
        }
      },
      {
        "patchline": {
          "source": ["obj-set-notes-text", 0],
          "destination": ["obj-notes-display", 0]
        }
      }
    ]
  }
}
