var Decimal = Java.type("java.math.BigDecimal");
if (request.getClass().getSimpleName() !== "ScriptRequest") { throw "wrong request type"; }
var amount = request.getAmount().add(new Decimal("2.50"));
var number = 5 + request.getIncrement();
logger.log("person=" + record.getValueInteger("id") + ";mode=" + deploymentMode);
record.setValue("lastName", request.getLabel() + ":" + amount.toPlainString() + ":" + number);
return record;
