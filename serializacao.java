Serialize:

ByteArrayOutputStream bStream = new ByteArrayOutputStream();
Output output = new Output(bStream);
Kryo kryo = new Kryo();
kryo.writeClassAndObject(output, a);
output.close();

byte[] serializedMessage = bStream.toByteArray();
System.out.println("Tamanho do byte: " +  serializedMessage.length);

DatagramPacket packetSend
        = new DatagramPacket(serializedMessage, serializedMessage.length, InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), 6002);
socket.send(packetSend);

Deserialize:

Kryo kryo = new Kryo();
socket.receive(packet);
ByteArrayInputStream bStream = new ByteArrayInputStream(buffer);
Input input = new Input(bStream);
Header header = (Header) kryo.readClassAndObject(input);
input.close();

if(header instanceof Alive){
    Alive alive = (Alive) header;
    System.out.println("Sou instancia do alive");
    System.out.println("ALIVE:::: " + alive);
}