{
  description = "Spider — VPN client";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachSystem [ "x86_64-linux" ] (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        version = "3.1.8";

        spider = pkgs.stdenv.mkDerivation {
          pname = "spider";
          inherit version;

          src = pkgs.fetchurl {
            url = "https://github.com/amirh00sain/spider-client/releases/download/v${version}/Spider-${version}-linux-x86_64.tar.gz";
            hash = "sha256-XPrxz7qdzFyaUoXN8WHhjifvnNPjFp9R+YXyeJamR68=";
          };

          nativeBuildInputs = with pkgs; [ autoPatchelfHook makeWrapper copyDesktopItems ];

          buildInputs = with pkgs; [
            alsa-lib at-spi2-atk cairo cups.lib dbus.lib expat fontconfig
            freetype gdk-pixbuf glib gtk3 libGL libxkbcommon nss nspr pango
            stdenv.cc.cc.lib xorg.libX11 xorg.libXext xorg.libXi xorg.libXrender
            xorg.libXtst zlib
          ];

          desktopItems = [
            (pkgs.makeDesktopItem {
              name = "spider";
              exec = "spider";
              icon = "spider";
              desktopName = "Spider";
              categories = [ "Network" ];
            })
          ];

          installPhase = ''
            runHook preInstall
            mkdir -p $out/share/spider $out/bin
            cp -r ./* $out/share/spider/

            bin=$(find $out/share/spider/bin -maxdepth 1 -type f -perm -u+x | head -1)
            makeWrapper "$bin" $out/bin/spider \
              --prefix LD_LIBRARY_PATH : ${pkgs.lib.makeLibraryPath [ pkgs.systemdLibs ]}

            icon=$(find $out/share/spider -name '*.png' | head -1)
            if [ -n "$icon" ]; then
              install -Dm444 "$icon" $out/share/icons/hicolor/512x512/apps/spider.png
            fi
            runHook postInstall
          '';

          meta = with pkgs.lib; {
            description = "VPN client with Xray, sing-box, Psiphon, Tor and DNS tunnels";
          homepage = "https://github.com/amirh00sain/spider-client";
            license = licenses.agpl3Plus;
            platforms = [ "x86_64-linux" ];
            mainProgram = "spider";
          };
        };
      in
      {
        packages.default = spider;
        packages.spider = spider;

        apps.default = flake-utils.lib.mkApp { drv = spider; };
      });
}
