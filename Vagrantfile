# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.network "private_network", ip: "10.10.10.10"
  config.vm.hostname = "hermes"
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4000"
    vb.name = "hermes"
  end
  config.vm.post_up_message = """


    ╥⌂
    ╫▓▄Z,
    ║▒▒▓▄Å╦
    ╚▒▒▓▓▌▄▀▄
     ▒▒▓▓▓▓▌▄▀▄
     ╡▀▓▓▓▓▓▓▓▄▀▌µ
     ╫▒╬▀▓▓▓▓▓▓▓▄▄▀▄▄
      ▓▓▓▄▒▀█▓▓▓▓▓▓▄▄▀█▄▄,
      ╙▓▓▓▓▓▒▄▒▀█▓▓▓▓▓▓▄▄▀▀█▓▓▌▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄,
        █▓▓▓▓▓▓▓▒▄▄▒▀▀█▓▓▓▌ ▒▄▄▄▄▌▌▌▌▌▌▌▌▌▌▌▀▀▀██▓▓▄▄
       '▄▄▀██▓▓▓▓▓▓▓▓▓▓▄▄▄Qδ ▀▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▌▄▌▀█▓▌▄
        └█▓▒▒▄▄▒▀▀▀███▓▓▓▓▓▓ ╚▄▀█▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▄▌█▓▌,
          ▀▓▓▓▓▓▓▓▓▓▓▒▒▄▄▄▄▄▄╕▀▓▓▌▄▄▌▀▀▀▀▌▌▌▌▄▄▌▌▌▌▀█▓▓▓▓▄▀▓▓╕
            ▀█▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓╦╙▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▄▌▀▓▓▄▀▓▌
           └▒▒▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▒▄  ▀▓▓▓▓▓▓▓▓▓▓▓███████▓▓▓▓▓▌▄█▓╝
             ╙█▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓█▀─╚▌▌▌▄▄▄▄▄▄▓▓▓▓▓▓▓▓▓▓▓▓▓▀▓▓▀
                `Σ▀▀▀▀▀▀Q▄▄▄▄▒▓▓▓▄╙▓▓▓▓▓▓▓▓▓▓▓███▀▀▀▀██▓▓δ
                 `╨▒▀█▓▓▓▓▓▓▓▓▓▓▓▓▌▄▀█▓▓█▀▀▄▄▒▀▀▀▀▀▀▀▀▀Γ
                     `²¥╬▌▀▀███████▀▀▄▄▄█▀Γ.
                            ΓΓ╙╨▀▀▀▀Γ
    ,▓Θ
    ▓▓▌
    ▓▓▓▓▓▓▌▄   ▄▓▓▓▓▌▄  ▓▓▌▓▓▌`╫▓▓▓▓▓▄▄▓▓▓▌╕  ,▄▓▓▓▓▌▄  ╓▄▓▓▓▓▌▄
    ▓▓▓   ▓▓µ╓▓▓▀   ▀▓▓ ▓▓▓Γ   █▓█  ▀▓▓Γ └▓▓µ╠▓▓Γ   █▓▓ ▓▓▌ . ▀
    ▓▓▌   ╫▓▌█▓▓▓▓▓▓▓▓▓∩▓▓µ    █▓▌  ]▓▓   ▓▓▌▓▓▓▓▓▓▓▓▓▓  ▀█▓▓▓▌╕
    ▓▓▌   ╫▓▌╙▓▓▄  ,▄▌▄ ▓▓µ    █▓▌  ]▓▓   ▓▓▌╙▓▓▄  ,▄▌╕ ,▄, ,╠▓▓
    ██Γ   ▀█▌  ▀█████▀  ██⌐    ██Θ  ╘██   ██Γ  ▀█████▀  ▀█████▀


        * Console:       http://10.10.10.10
        * Frontend:      http://10.10.10.10:8080
        * Management:    http://10.10.10.10:8090
        * Graphite:      http://10.10.10.10:8082

   """

  config.vm.provision "shell", path: "vagrant_provisioning/init.sh"

  config.vm.provision "puppet", manifests_path: "vagrant_provisioning", manifest_file: "graphite.pp", run: "always"

  config.vm.provision "shell", path: "vagrant_provisioning/start.sh", run: "always"

  config.vm.provision "frontend", type: "shell", path: "vagrant_provisioning/install_hermes_module.sh", args: "frontend"
  config.vm.provision "consumers", type: "shell", path: "vagrant_provisioning/install_hermes_module.sh", args: "consumers"
  config.vm.provision "management", type: "shell", path: "vagrant_provisioning/install_hermes_module.sh", args: "management"

  config.vm.provision "console", type: "shell", path: "vagrant_provisioning/install_console.sh"

end
