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
    "rect": [80.0, 80.0, 980.0, 620.0],
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
          "id": "obj-bg",
          "maxclass": "panel",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [15.0, 15.0, 560.0, 170.0],
          "presentation": 1,
          "presentation_rect": [0.0, 0.0, 560.0, 170.0],
          "bgcolor": [0.1, 0.12, 0.15, 1.0],
          "rounded": 8
        }
      },
      {
        "box": {
          "id": "obj-header",
          "maxclass": "comment",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [25.0, 20.0, 340.0, 20.0],
          "presentation": 1,
          "presentation_rect": [14.0, 8.0, 340.0, 20.0],
          "text": "SPATIAL-LAB 3D MkII — BINAURAL HRTF & ORBIT ENGINE",
          "textcolor": [0.28, 0.96, 0.78, 1.0],
          "fontsize": 11.0
        }
      },
      {
        "box": {
          "id": "obj-coords-readout",
          "maxclass": "comment",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [600.0, 320.0, 320.0, 24.0],
          "presentation": 1,
          "presentation_rect": [14.0, 30.0, 210.0, 24.0],
          "text": "PAD 1: Frente Cercano (0°)",
          "textcolor": [1.0, 0.76, 0.28, 1.0],
          "fontsize": 12.0
        }
      },
      {
        "box": {
          "id": "obj-dial-x",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [40.0, 60.0, 50.0, 48.0],
          "presentation": 1,
          "presentation_rect": [14.0, 58.0, 55.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "3D Position X (Left/Right)",
              "parameter_shortname": "X (L/R)",
              "parameter_type": 0,
              "parameter_mmin": -1.0,
              "parameter_mmax": 1.0,
              "parameter_initial_enable": 1,
              "parameter_initial": [0.0],
              "parameter_unitstyle": 1
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prep-x",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [40.0, 120.0, 75.0, 20.0],
          "text": "prepend pos_x"
        }
      },
      {
        "box": {
          "id": "obj-dial-y",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [125.0, 60.0, 50.0, 48.0],
          "presentation": 1,
          "presentation_rect": [76.0, 58.0, 55.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "3D Position Y (Elevation)",
              "parameter_shortname": "Y (Elev)",
              "parameter_type": 0,
              "parameter_mmin": -1.0,
              "parameter_mmax": 1.0,
              "parameter_initial_enable": 1,
              "parameter_initial": [0.2],
              "parameter_unitstyle": 1
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prep-y",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [125.0, 120.0, 75.0, 20.0],
          "text": "prepend pos_y"
        }
      },
      {
        "box": {
          "id": "obj-dial-z",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [210.0, 60.0, 50.0, 48.0],
          "presentation": 1,
          "presentation_rect": [138.0, 58.0, 55.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "3D Position Z (Front/Back)",
              "parameter_shortname": "Z (Depth)",
              "parameter_type": 0,
              "parameter_mmin": -1.0,
              "parameter_mmax": 1.0,
              "parameter_initial_enable": 1,
              "parameter_initial": [0.55],
              "parameter_unitstyle": 1
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prep-z",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [210.0, 120.0, 75.0, 20.0],
          "text": "prepend pos_z"
        }
      },
      {
        "box": {
          "id": "obj-dial-ospeed",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [295.0, 60.0, 55.0, 48.0],
          "presentation": 1,
          "presentation_rect": [210.0, 58.0, 60.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "3D Orbit Speed",
              "parameter_shortname": "Orbit Hz",
              "parameter_type": 0,
              "parameter_mmin": 0.0,
              "parameter_mmax": 4.0,
              "parameter_initial_enable": 1,
              "parameter_initial": [0.0],
              "parameter_unitstyle": 3
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prep-ospeed",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [295.0, 120.0, 105.0, 20.0],
          "text": "prepend orbit_speed"
        }
      },
      {
        "box": {
          "id": "obj-dial-odepth",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [410.0, 60.0, 55.0, 48.0],
          "presentation": 1,
          "presentation_rect": [278.0, 58.0, 60.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "3D Orbit Radius",
              "parameter_shortname": "Radius",
              "parameter_type": 0,
              "parameter_mmin": 0.0,
              "parameter_mmax": 1.0,
              "parameter_initial_enable": 1,
              "parameter_initial": [0.0],
              "parameter_unitstyle": 5
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prep-odepth",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [410.0, 120.0, 105.0, 20.0],
          "text": "prepend orbit_depth"
        }
      },
      {
        "box": {
          "id": "obj-dial-room",
          "maxclass": "live.dial",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", "float"],
          "parameter_enable": 1,
          "patching_rect": [525.0, 60.0, 55.0, 48.0],
          "presentation": 1,
          "presentation_rect": [346.0, 58.0, 60.0, 48.0],
          "saved_attribute_attributes": {
            "valueof": {
              "parameter_longname": "Room Reflections Mix",
              "parameter_shortname": "Room 3D",
              "parameter_type": 0,
              "parameter_mmin": 0.0,
              "parameter_mmax": 1.0,
              "parameter_initial_enable": 1,
              "parameter_initial": [0.3],
              "parameter_unitstyle": 5
            }
          }
        }
      },
      {
        "box": {
          "id": "obj-prep-room",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [525.0, 120.0, 95.0, 20.0],
          "text": "prepend room_mix"
        }
      },
      {
        "box": {
          "id": "obj-ctlin1",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["int", "int"],
          "patching_rect": [635.0, 60.0, 46.0, 20.0],
          "text": "ctlin 1"
        }
      },
      {
        "box": {
          "id": "obj-prep-cc1",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [635.0, 120.0, 68.0, 20.0],
          "text": "prepend cc1"
        }
      },
      {
        "box": {
          "id": "obj-notein",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 3,
          "outlettype": ["int", "int", "int"],
          "patching_rect": [720.0, 60.0, 46.0, 20.0],
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
          "patching_rect": [720.0, 90.0, 56.0, 20.0],
          "text": "pack 0 0"
        }
      },
      {
        "box": {
          "id": "obj-prep-note",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [720.0, 120.0, 74.0, 20.0],
          "text": "prepend note"
        }
      },
      {
        "box": {
          "id": "obj-metro",
          "maxclass": "newobj",
          "numinlets": 2,
          "numoutlets": 1,
          "outlettype": ["bang"],
          "patching_rect": [815.0, 60.0, 98.0, 20.0],
          "text": "metro 25 @active 1"
        }
      },
      {
        "box": {
          "id": "obj-msg-tick",
          "maxclass": "message",
          "numinlets": 2,
          "numoutlets": 1,
          "outlettype": [""],
          "patching_rect": [815.0, 120.0, 34.0, 20.0],
          "text": "tick"
        }
      },
      {
        "box": {
          "id": "obj-js-spatial",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 2,
          "outlettype": ["", ""],
          "patching_rect": [295.0, 195.0, 155.0, 20.0],
          "saved_object_attributes": {
            "filename": "spatiallab_m4l.js",
            "parameter_enable": 0
          },
          "text": "js spatiallab_m4l.js"
        }
      },
      {
        "box": {
          "id": "obj-unpack-dsp",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 7,
          "outlettype": ["float", "float", "float", "float", "float", "float", "float"],
          "patching_rect": [180.0, 240.0, 220.0, 20.0],
          "text": "unpack 0. 0. 0.7 0.7 12000. 12000. 0.2"
        }
      },
      {
        "box": {
          "id": "obj-plugin-in",
          "maxclass": "newobj",
          "numinlets": 2,
          "numoutlets": 2,
          "outlettype": ["signal", "signal"],
          "patching_rect": [40.0, 240.0, 56.0, 20.0],
          "text": "plugin~"
        }
      },
      {
        "box": {
          "id": "obj-tapin-l",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": ["tapconnect"],
          "patching_rect": [40.0, 295.0, 68.0, 20.0],
          "text": "tapin~ 50"
        }
      },
      {
        "box": {
          "id": "obj-tapout-l",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": ["signal"],
          "patching_rect": [40.0, 330.0, 68.0, 20.0],
          "text": "tapout~ 0."
        }
      },
      {
        "box": {
          "id": "obj-tapin-r",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": ["tapconnect"],
          "patching_rect": [125.0, 295.0, 68.0, 20.0],
          "text": "tapin~ 50"
        }
      },
      {
        "box": {
          "id": "obj-tapout-r",
          "maxclass": "newobj",
          "numinlets": 1,
          "numoutlets": 1,
          "outlettype": ["signal"],
          "patching_rect": [125.0, 330.0, 68.0, 20.0],
          "text": "tapout~ 0."
        }
      },
      {
        "box": {
          "id": "obj-lp-l",
          "maxclass": "newobj",
          "numinlets": 2,
          "numoutlets": 1,
          "outlettype": ["signal"],
          "patching_rect": [40.0, 375.0, 82.0, 20.0],
          "text": "onepole~ 12000."
        }
      },
      {
        "box": {
          "id": "obj-lp-r",
          "maxclass": "newobj",
          "numinlets": 2,
          "numoutlets": 1,
          "outlettype": ["signal"],
          "patching_rect": [125.0, 375.0, 82.0, 20.0],
          "text": "onepole~ 12000."
        }
      },
      {
        "box": {
          "id": "obj-gain-l",
          "maxclass": "newobj",
          "numinlets": 2,
          "numoutlets": 1,
          "outlettype": ["signal"],
          "patching_rect": [40.0, 420.0, 46.0, 20.0],
          "text": "*~ 0.7"
        }
      },
      {
        "box": {
          "id": "obj-gain-r",
          "maxclass": "newobj",
          "numinlets": 2,
          "numoutlets": 1,
          "outlettype": ["signal"],
          "patching_rect": [125.0, 420.0, 46.0, 20.0],
          "text": "*~ 0.7"
        }
      },
      {
        "box": {
          "id": "obj-plugout",
          "maxclass": "newobj",
          "numinlets": 2,
          "numoutlets": 0,
          "patching_rect": [40.0, 480.0, 62.0, 20.0],
          "text": "plugout~"
        }
      },
      {
        "box": {
          "id": "obj-info",
          "maxclass": "comment",
          "numinlets": 1,
          "numoutlets": 0,
          "patching_rect": [240.0, 480.0, 520.0, 40.0],
          "presentation": 1,
          "presentation_rect": [14.0, 114.0, 530.0, 46.0],
          "text": "• Knobs 1–3 = Coordenadas X (Izq/Der), Y (Elevación), Z (Frente/Detrás)  |  Knobs 4–6 = Órbita 3D & Sala\n• Tira Modulación (CC 1) = Gira la fuente 360° alrededor de tu cabeza\n• Pads 1–8 (Notas 36–43) = 8 posiciones y órbitas 3D instantáneas (Frente, Nuca, Cenit, Órbita 360, Infinito 8, Espiral)",
          "textcolor": [0.75, 0.78, 0.82, 1.0],
          "fontsize": 9.2
        }
      }
    ],
    "lines": [
      { "patchline": { "source": ["obj-dial-x", 0], "destination": ["obj-prep-x", 0] } },
      { "patchline": { "source": ["obj-prep-x", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-dial-y", 0], "destination": ["obj-prep-y", 0] } },
      { "patchline": { "source": ["obj-prep-y", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-dial-z", 0], "destination": ["obj-prep-z", 0] } },
      { "patchline": { "source": ["obj-prep-z", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-dial-ospeed", 0], "destination": ["obj-prep-ospeed", 0] } },
      { "patchline": { "source": ["obj-prep-ospeed", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-dial-odepth", 0], "destination": ["obj-prep-odepth", 0] } },
      { "patchline": { "source": ["obj-prep-odepth", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-dial-room", 0], "destination": ["obj-prep-room", 0] } },
      { "patchline": { "source": ["obj-prep-room", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-ctlin1", 0], "destination": ["obj-prep-cc1", 0] } },
      { "patchline": { "source": ["obj-prep-cc1", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-notein", 0], "destination": ["obj-pack-note", 0] } },
      { "patchline": { "source": ["obj-notein", 1], "destination": ["obj-pack-note", 1] } },
      { "patchline": { "source": ["obj-pack-note", 0], "destination": ["obj-prep-note", 0] } },
      { "patchline": { "source": ["obj-prep-note", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-metro", 0], "destination": ["obj-msg-tick", 0] } },
      { "patchline": { "source": ["obj-msg-tick", 0], "destination": ["obj-js-spatial", 0] } },
      { "patchline": { "source": ["obj-js-spatial", 0], "destination": ["obj-unpack-dsp", 0] } },
      { "patchline": { "source": ["obj-plugin-in", 0], "destination": ["obj-tapin-l", 0] } },
      { "patchline": { "source": ["obj-plugin-in", 1], "destination": ["obj-tapin-r", 0] } },
      { "patchline": { "source": ["obj-tapin-l", 0], "destination": ["obj-tapout-l", 0] } },
      { "patchline": { "source": ["obj-tapin-r", 0], "destination": ["obj-tapout-r", 0] } },
      { "patchline": { "source": ["obj-unpack-dsp", 0], "destination": ["obj-tapout-l", 0] } },
      { "patchline": { "source": ["obj-unpack-dsp", 1], "destination": ["obj-tapout-r", 0] } },
      { "patchline": { "source": ["obj-tapout-l", 0], "destination": ["obj-lp-l", 0] } },
      { "patchline": { "source": ["obj-tapout-r", 0], "destination": ["obj-lp-r", 0] } },
      { "patchline": { "source": ["obj-unpack-dsp", 4], "destination": ["obj-lp-l", 1] } },
      { "patchline": { "source": ["obj-unpack-dsp", 5], "destination": ["obj-lp-r", 1] } },
      { "patchline": { "source": ["obj-lp-l", 0], "destination": ["obj-gain-l", 0] } },
      { "patchline": { "source": ["obj-lp-r", 0], "destination": ["obj-gain-r", 0] } },
      { "patchline": { "source": ["obj-unpack-dsp", 2], "destination": ["obj-gain-l", 1] } },
      { "patchline": { "source": ["obj-unpack-dsp", 3], "destination": ["obj-gain-r", 1] } },
      { "patchline": { "source": ["obj-gain-l", 0], "destination": ["obj-plugout", 0] } },
      { "patchline": { "source": ["obj-gain-r", 0], "destination": ["obj-plugout", 1] } }
    ]
  }
}
