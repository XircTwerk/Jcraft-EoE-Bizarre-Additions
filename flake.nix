{
  description = "Java dev environment";

  inputs = {
    nixpkgs = {
      url = "github:nixos/nixpkgs/nixos-unstable";
    };
    flake-utils = {
      url = "github:numtide/flake-utils";
    };
  };
  outputs =
    { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import nixpkgs { inherit system; };

        runClient = pkgs.writeShellScriptBin "runClient" (''gradle runClient'');
        runServer = pkgs.writeShellScriptBin "runClient" (''gradle runServer'');

      in
      rec {
        devShell = pkgs.mkShell {
          shellHook = ''
            export LD_LIBRARY_PATH="''${LD_LIBRARY_PATH}''${LD_LIBRARY_PATH:+:}${pkgs.libglvnd}/lib"
          '';

          buildInputs = with pkgs; [
            jdk17
            gradle_8
            zenity

            runClient
            runServer

            libpulseaudio
            libGL
            glfw
            openal
            stdenv.cc.cc.lib
            jdk21
            flite

          ];
        };
      }
    );
}
