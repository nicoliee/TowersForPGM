package org.nicolie.towersforpgm.commandUtils;

import org.bukkit.command.CommandSender;

public class HelpConfig {

    public static void sendDraftHelp(CommandSender sender) {
        sender.sendMessage("§6§m--------------------§6§lDraft §6§m--------------------");
        sender.sendMessage("§6/towers §bdraft §asuggestions §7- Muestra sugerencias de configuración para el draft.");
        sender.sendMessage("§6/towers §bdraft §atimer §7- Muestra o modifica el temporizador del draft.");
        sender.sendMessage("§6/towers §bdraft §aprivate <true/false> §7- Define si el draft es privado.");
        sender.sendMessage("§6/towers §bdraft §aorder <A[AB]+> §7- Establece el orden del draft.");
        sender.sendMessage("§6/towers §bdraft §amin <size> §7- Establece el tamaño mínimo del draft. para aplicar el orden custom.");
    }

    public static void sendPreparationHelp(CommandSender sender) {
        sender.sendMessage("§6§m--------------------§6§lPreparation §6§m--------------------");
        sender.sendMessage("§6/towers §bpreparation §aadd §7- Añade una nueva configuración de preparación.");
        sender.sendMessage("§6/towers §bpreparation §aremove §7- Elimina una configuración de preparación.");
        sender.sendMessage("§6/towers §bpreparation §amax <x> <y> <z> §7- Define la esquina máxima del área.");
        sender.sendMessage("§6/towers §bpreparation §amin <x> <y> <z> §7- Define la esquina mínima del área.");
        sender.sendMessage("§6/towers §bpreparation §atimer <mins> §7- Establece el tiempo de preparación en minutos.");
        sender.sendMessage("§6/towers §bpreparation §ahaste <mins> §7- Establece la duración del efecto de haste.");
        sender.sendMessage("§6/towers §bpreparation §alist §7- Lista todas las configuraciones de preparación.");
    }

    public static void sendRefillHelp(CommandSender sender) {
        sender.sendMessage("§6§m--------------------§6§lRefill §6§m--------------------");
        sender.sendMessage("§6/towers §brefill §aadd §7- Añade un cofre a la lista de refill.");
        sender.sendMessage("§6/towers §brefill §aremove §7- Elimina un cofre de la lista de refill.");
        sender.sendMessage("§6/towers §brefill §areload §7- Recarga la configuración de refill.");
    }

    public static void sendStatsHelp(CommandSender sender) {
        sender.sendMessage("§6§m--------------------§6§lStats §6§m--------------------");
        sender.sendMessage("§6/towers §bstats §atoggle §7- Activa o desactiva la visualización de estadísticas.");
        sender.sendMessage("§6/towers §bstats §adefault <tabla> §7- Establece una tabla por defecto.");
        sender.sendMessage("§6/towers §bstats §aadd <tabla> §7- Añade una tabla de estadísticas.");
        sender.sendMessage("§6/towers §bstats §aremove <tabla> §7- Elimina una tabla de estadísticas.");
        sender.sendMessage("§6/towers §bstats §alist §7- Muestra las tablas registradas.");
        sender.sendMessage("§6/towers §bstats §aaddMap <tabla> §7- Asocia un mapa a una tabla.");
        sender.sendMessage("§6/towers §bstats §aremoveMap <tabla> §7- Desasocia un mapa de una tabla.");
    }

    public static void sendGeneralHelp(CommandSender sender) {
        sender.sendMessage("§6§m--------------------§6§lTowers §6§m--------------------");
        sender.sendMessage("§6Uso general de /towers:");
        sender.sendMessage("§6/towers §bdraft §7- Opciones relacionadas al draft.");
        sender.sendMessage("§6/towers §bpreparation §7- Configuraciones de preparación del mapa.");
        sender.sendMessage("§6/towers §bstats §7- Gestión de tablas y estadísticas.");
        sender.sendMessage("§6/towers §bhelp <suggestions|preparation|stats> §7- Muestra ayuda detallada.");
    }
}
