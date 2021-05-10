package peer;

public class MessageStuffer {
    private static byte flag = 0x7e;
    private static byte esc1 = 0x7d;
    private static byte esc2 = 0x5e;
    private static byte esc3 = 0x5d;

    public static byte[] unstuffMessage(byte[] message) {
        for (int i = 0; i < message.length; i++) {
            if (message[i] == esc1 && message[i + 1] == esc2)
            {
                byte[] auxBuffer = new byte[message.length - 1];
                System.arraycopy(message, 0, auxBuffer, 0, i);
                auxBuffer[i] = flag;
                System.arraycopy(message, i + 2, auxBuffer, i + 1, message.length - i - 2);
                message = auxBuffer;
            }
            else if (message[i] == esc1 && message[i + 1] == esc3)
            {
                byte[] auxBuffer = new byte[message.length - 1];
                System.arraycopy(message, 0, auxBuffer, 0, i);
                auxBuffer[i] = esc1;
                System.arraycopy(message, i + 2, auxBuffer, i + 1, message.length - i - 2);
                message = auxBuffer;
            }
        }
        return message;
    }

    public static byte[] stuffMessage(byte[] message) {
        for (int i = 0; i < message.length; i++) {
            if (message[i] == flag)
            {
                byte[] auxBuffer = new byte[message.length + 1];
                System.arraycopy(message, 0, auxBuffer, 0, i);
                auxBuffer[i] = esc1;
                auxBuffer[i + 1] = esc2;
                System.arraycopy(message, i + 1, auxBuffer, i + 2, message.length - i - 1);
                message = auxBuffer;
                i++;
            }
            else if (message[i] == esc1)
            {
                byte[] auxBuffer = new byte[message.length + 1];
                System.arraycopy(message, 0, auxBuffer, 0, i);
                auxBuffer[i] = esc1;
                auxBuffer[i + 1] = esc3;
                System.arraycopy(message, i + 1, auxBuffer, i + 2, message.length - i - 1);
                message = auxBuffer;
                i++;
            }
        }
        return message;
    }
}
