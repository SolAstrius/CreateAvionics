{
  description = "Create: Avionics — ComputerCraft peripherals for Create: Simulated and Create: Aeronautics";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        jdk = pkgs.jdk21;

        # LWJGL dlopens these at runtime; nix-built Java doesn't inherit FHS paths.
        lwjglRuntimeLibs = with pkgs; [
          libglvnd
          libx11
          libxext
          libxrandr
          libxcursor
          libxxf86vm
          libxi
          libxinerama
          libxkbcommon
          libpulseaudio
          alsa-lib
          openal
          dbus
          fontconfig
          freetype
          udev
          stdenv.cc.cc.lib
        ];
      in {
        devShells.default = pkgs.mkShell {
          name = "create-avionics";
          packages = [ jdk ];

          shellHook = ''
            export JAVA_HOME=${jdk}/lib/openjdk
            # /run/opengl-driver/lib comes from hardware.graphics and provides the
            # active GPU vendor's GLX/EGL. Keep it first so the vendor ICD wins.
            export LD_LIBRARY_PATH=/run/opengl-driver/lib:${pkgs.lib.makeLibraryPath lwjglRuntimeLibs}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}
          '';
        };
      });
}
