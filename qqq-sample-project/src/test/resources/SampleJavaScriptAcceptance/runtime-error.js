record.setValue("lastName", "not-persisted");
logger.log("before-owned-failure");
throw "owned script failure";
