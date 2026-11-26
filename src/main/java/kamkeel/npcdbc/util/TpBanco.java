package kamkeel.npcdbc.util;

import java.lang.reflect.Method;

import JinRyuu.JRMCore.JRMCoreH;
import net.minecraft.entity.player.EntityPlayer;

/**
 * DBR: puente suave a DbrTpBanco (DbrServerPack/DbrTpBanco). El TP de un personaje es
 * {@code jrmcTpint} (billetera) + {@code dbrTpBanco} (banco long). Sin el mod, todo cae a la
 * billetera de JRMCore como antes.
 */
public final class TpBanco {

    private static final long TOPE_JRMCORE = 2000000000L;

    private static boolean buscado;
    private static Method totalCliente;
    private static Method totalServidor;

    private TpBanco() {}

    private static void buscar() {
        if (buscado) return;
        buscado = true;
        try {
            totalServidor = Class.forName("dbr.machitos.tpbanco.api.TpBancoApi")
                .getMethod("total", EntityPlayer.class);
        } catch (Throwable ignorada) {
            totalServidor = null;
        }
        try {
            totalCliente = Class.forName("dbr.machitos.tpbanco.cliente.TpBancoCliente").getMethod("total");
        } catch (Throwable ignorada) {
            totalCliente = null;
        }
    }

    /** Total del jugador local; {@code JRMCoreH.curTP} si el servidor no manda el total. */
    public static long totalCliente() {
        buscar();
        if (totalCliente != null) {
            try {
                long t = (Long) totalCliente.invoke(null);
                if (t >= 0) return t;
            } catch (Throwable ignorada) {
                // cae a la billetera
            }
        }
        return JRMCoreH.curTP;
    }

    /** Total de un jugador en el servidor; la billetera si DbrTpBanco no esta. */
    public static long total(EntityPlayer p) {
        buscar();
        if (totalServidor != null) {
            try {
                return (Long) totalServidor.invoke(null, p);
            } catch (Throwable ignorada) {
                // cae a la billetera
            }
        }
        return JRMCoreH.getInt(p, "jrmcTpint");
    }

    /**
     * Coste de mejora como {@code JRMCoreH.attrCst(attrs, ma)} pero sin el tope de 2e9 (DbrTpBanco
     * cobra el resto del total en el servidor). 0 si el atributo esta al maximo.
     */
    public static long costeMejora(int[] attrs, int ma) {
        // Sin mirar attrCst(attrs, ma): suma en int y por encima de 2^31 da la vuelta (x1000 de
        // 4,68e9 devuelve 386 M). La suma en long es la misma cuenta.
        int att = JRMCoreH.attributeCostAboveLimit(attrs);
        if (att >= TOPE_JRMCORE || att <= 0) return 0;
        int veces = JRMCoreH.attributeMultiplier(ma);
        long suma = 0;
        for (int j = 0; j < veces; j++) {
            int c = JRMCoreH.attrCst(att + j);
            if (c == 0) return 0;
            suma += c;
        }
        return suma;
    }
}
