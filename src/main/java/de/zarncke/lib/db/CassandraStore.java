package de.zarncke.lib.db;


public class CassandraStore<S, T> {
	// public static final Context<ConnectionPool> CTX = Context.of(Default.of(null, ConnectionPool.class));

	// {
	// PoolConfiguration pc = new PoolProperties();
	// ConnectionPool cp = new ConnectionPool(pc);
	// CassandraDaemon cd = new CassandraDaemon();
	// cd.start();
	//
	// TTransport tr = new TFramedTransport(new TSocket("localhost", 9160));
	// TProtocol proto = new TBinaryProtocol(tr);
	//
	// Cassandra.Client client = new Cassandra.Client(proto);
	//
	// private static final ColumnParent IDMP_COLUMN_PARENT = new ColumnParent("idmap");
	// private final byte[] columnName;
	// private final ColumnPath columnPath;
	//
	// private final Class<S> sourceClass;
	//
	// private final Class<T> targetClass;
	//
	// public CassandraStore(final Class<S> sourceType, final Class<T> targetType) {
	// this.sourceClass = sourceType;
	// this.targetClass = targetType;
	// this.columnName = (sourceType.getName() + "-" + targetType.getName()).getBytes();
	// this.columnPath = new ColumnPath("idmap").setColumn(this.columnName);
	// }
	//
	// public Gid<T> get(final Gid<S> id) {
	// Cassandra.Client client;
	// try {
	// client = CTX.get().getConnection();
	// } catch (TException e) {
	// throw Warden.spot(new NotAvailableException("failed to get connection", e));
	// }
	// ColumnOrSuperColumn value;
	// try {
	// value = client.get(ByteBuffer.wrap(id.getIdAsBytes()), this.columnPath, ConsistencyLevel.QUORUM);
	// } catch (InvalidRequestException e) {
	// throw Warden.spot(new IllegalStateException("schema not properly set up?", e));
	// } catch (NotFoundException e) {
	// return null;
	// } catch (UnavailableException e) {
	// throw Warden.spot(new NotAvailableException("no response", e));
	// } catch (TimedOutException e) {
	// throw Warden.spot(new NotAvailableException("no timely answer", e));
	// } catch (TException e) {
	// throw Warden.spot(new NotAvailableException("unknown problem", e));
	// }
	// return Gid.of(value.getColumn().getValue(), this.targetClass);
	// }
	//
	// public void put(final Gid<S> id, final Gid<T> value) {
	// Cassandra.Client client;
	// try {
	// client = CTX.get().getConnection();
	// } catch (TException e) {
	// throw Warden.spot(new NotAvailableException("failed to get connection", e));
	// }
	// long timestamp = System.currentTimeMillis();
	// Column valueColumn = new Column(ByteBuffer.wrap(this.columnName));
	// valueColumn.setValue(value.getIdAsBytes());
	// valueColumn.setTimestamp(timestamp);
	// try {
	// client.insert(ByteBuffer.wrap(id.getIdAsBytes()), CassandraStore.IDMP_COLUMN_PARENT, valueColumn,
	// ConsistencyLevel.ALL);
	// } catch (InvalidRequestException e) {
	// throw Warden.spot(new IllegalStateException("schema not properly set up?", e));
	// } catch (UnavailableException e) {
	// throw Warden.spot(new NotAvailableException("no response", e));
	// } catch (TimedOutException e) {
	// throw Warden.spot(new NotAvailableException("no timely answer", e));
	// } catch (TException e) {
	// throw Warden.spot(new NotAvailableException("unknown problem", e));
	// }
	// }
}
