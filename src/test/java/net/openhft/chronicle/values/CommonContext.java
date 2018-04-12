package net.openhft.chronicle.values;


import net.openhft.chronicle.bytes.Byteable;

public interface CommonContext extends Byteable {
	//int COMMON_GROUP_OFFSET = 0;
	/**
	 * OrderType
	 */
	enum OrderType {
		MARKET,
		LIMIT
	}

	/**
	 * ConditionalStatus
	 */
	enum ConditionalStatus  {
		NORNAL,
		CONDITIONAL,
		INVITE,
		FIRM,
		MATCH_STATUS
	}

	/**
	 * The common order side
	 */
	enum OrderSide {
		BUY,
		BUY_MINUS,
		SELL,
		SELL_PLUS,
		SELL_SHORT,
		SELL_SHORT_EXEMPT
	}

	/**
	 * Order Capacity
	 */
	enum OrderCapacity {
		AGENCY,
		SHORT_EXEMPT_TRANSACTION_A_TYPE,
		PROPRIETARY_NON_ALGO,
		PROGRAM_ORDER_MEMBER,
		SHORT_EXEMPT_TRANSACTION_FOR_PRINCIPAL,
		SHORT_EXEMPT_TRANSACTION_W_TYPE,
		SHORT_EXEMPT_TRANSACTION_I_TYPE,
		INDIVIDUAL,
		PROPRIETARY_ALGO,
		AGENCY_ALGO,
		SHORT_EXEMPT_TRANSACTION_MEMBER_AFFLIATED,
		PROGRAM_ORDER_OTHER_MEMBER,
		AGENT_FOR_OTHER_MEMBER,
		PROPRIETARY_TRANSACTION_AFFLIATED,
		PRINCIPAL,
		TRANSACTION_NON_MEMBER,
		SPECIALIST_TRADES,
		TRANSACTION_UNAFFLIATED_MEMBER,
		AGENACY_INDEX_ARB,
		ALL_OTHER_ORDERS_AS_AGENT_FOR_OTHER_MEMBER,
		SHORT_EXEMPT_TRANSACTION_MEMBER_NOT_AFFLIATED,
		AGENCY_NON_ALGO,
		SHORT_EXEMPT_TRANSACTION_NON_MEMBER
	}

	/**
	 * OrderStatus
	 */
	enum OrderStatus {
		PENDING_ORDER,
		NEW,
		PARTIALLY_FILLED,
		FILLED,
		AWAITING_CANCEL_ACK,
		PENDING_CANCELLED,
		CANCELLED,
		AWAITING_REPLACE_ACK,
		PENDING_REPLACED,
		REPLACED,
		REJECTED,
		DONE_FOR_DAY,
		STOPPED,
		DONE,
		CANCEL_REJECT,
		OUT,
		CONDITIONAL_FIRMING_UP,
		CONDITIONAL_FIRM
	}

	/**
	 * The Price Instruction
	 */
	enum PriceInstruction {
		MID,
		SPREAD
	}

	/**
	 * The ATS Match Status
	 */
	enum ATSMatchStatus {
		NONE,
		NO_CONTRA,
		PASSIVE_CONTRA,
		ACTIVE_CONTRA,
		NEGOTIATION_STARTED,
		NEGOTIATION_ENDED,
		BROKER_BLOCK_MATCH
	}

	/**
	 * The direction as mandated by drop Copy specifications
	 * The direction is WRT to Algo-container
	 */
	enum Direction {
		IN,
		OUT,
		INTERNAL
	}

	/**
	 * The Type of context
	 */
	enum ContextType {
		ORDER,
		EXECUTION
	}

	/**
	 * The Order Id
	 * @param id
	 */
	default void setOrderId(long id) {
		setContextKey(id);
	}
	default long getOrderId() {
		return getContextKey();
	}


	/**
	 * The Original Order Id (Cancelled order)
	 * @param id
	 */
	//@Group(0)
	void setOriginalOrderId(long id);
	long getOriginalOrderId();


	/**
	 * The Context Type
	 * @param contextType
	 */
	//@Group(1)
	void setContextType(ContextType contextType);
	ContextType getContextType();

	/**
	 * The Unique Systems Instance Id
	 * @param senderCompId
	 */
	//@Group(2)
	void setSystemInstanceId(@MaxUtf8Length(64) CharSequence senderCompId);
	CharSequence getSystemInstanceId();
	StringBuilder getUsingSystemInstanceId(StringBuilder senderCompId);

	/**
	 * The System Region
	 */
	enum TradingSystemRegion {
		EU, US, GLOB
	}

	/**
	 * The TradingSystem
	 */
	enum TradingSystem {
		EXT, ATS, IRHT, IRLT, IRGB, SOR, BRP, SELF, LNAC
	}

	/**
	 * The SenderCompId
	 */
	//@Group(3)
	void setSenderCompId(@MaxUtf8Length(64) CharSequence senderCompId);
	CharSequence getSenderCompId();
	StringBuilder getUsingSenderCompId(StringBuilder senderCompId);

	/**
	 * The SenderSubId
	 */
	//@Group(4)
	void setSenderSubId(@MaxUtf8Length(64) CharSequence senderSubId);
	CharSequence getSenderSubId();
	StringBuilder getUsingSenderSubId(StringBuilder senderSubId);

	/**
	 * The TargetCompId
	 */
	//@Group(5)
	void setTargetCompId(@MaxUtf8Length(64) CharSequence targetCompId);
	CharSequence getTargetCompId();
	StringBuilder getUsingTargetCompId(StringBuilder targetCompId);

	/**
	 * The OnBehalfOfCompID
	 */
	//@Group(6)
	void setOnBehalfOfCompID(@MaxUtf8Length(64) CharSequence onBehalfOfCompID);
	CharSequence getOnBehalfOfCompID();
	StringBuilder getUsingOnBehalfOfCompID(StringBuilder onBehalfOfCompID);

	/**
	 * The DeliverToCompID
	 */
	//@Group(7)
	void setDeliverToCompID(@MaxUtf8Length(64) CharSequence deliverToCompID);
	CharSequence getDeliverToCompID();
	StringBuilder getUsingDeliverToCompID(StringBuilder deliverToCompID);

	/**
	 * The Client Order Id
	 */
	//@Group(8)
	void setClientOrderId(@MaxUtf8Length(64) CharSequence clientOrderId);
	CharSequence getClientOrderId();
	StringBuilder getUsingClientOrderId(StringBuilder clientOrderId);

	/**
	 * The Original Client Order Id
	 */
	//@Group(9)
	void setOriginalClientOrderId(@MaxUtf8Length(64) CharSequence origClientOrderId);
	CharSequence getOriginalClientOrderId();
	StringBuilder getUsingOriginalClientOrderId(StringBuilder origClientOrderId);

	/**
	 * OrderSide
	 * @param orderSide
	 */
	//@Group(10)
	void setOrderSide(OrderSide orderSide);
	OrderSide getOrderSide();

	/**
	 * OrderCapacity
	 * @param orderCapacity
	 */
	//@Group(11)
	void setOrderCapacity(OrderCapacity orderCapacity);
	OrderCapacity getOrderCapacity();

	/**
	 * OrderStatus
	 * @param OrderStatus
	 */
	//@Group(12)
	void setOrderStatus(OrderStatus status);
	OrderStatus getOrderStatus();

	/**
	 * OrderType
	 * @param OrderType
	 */
	//@Group(13)
	void setOrderType(OrderType t);
	OrderType getOrderType();

	/**
	 * ConditionalStatus
	 * @param ConditionalStatus
	 */
	//@Group(14)
	void setConditionalStatus(ConditionalStatus t);
	ConditionalStatus getConditionalStatus();

	/**
	 * sets the limit price
	 * @param limit
	 */
	//@Group(15)
	void setLimitPrice(double limit);
	double getLimitPrice();

	/**
	 * Sets the Target quantity of the order
	 * @param target
	 */
	//@Group(16)
	void setTargetQuantity(int target);
	int getTargetQuantity();

	/**
	 * The direction of the order/execution context
	 * @param direction
	 */
	//@Group(17)
	void setDirection(Direction direction);
	Direction getDirection();

	/**
	 * The Region
	 * @param region
	 */
	//@Group(18)
	void setTradingSystemRegion(TradingSystemRegion region);
	TradingSystemRegion getTradingSystemRegion();

	/**
	 * The System Type
	 */
	//@Group(19)
	void setTradingSystem(TradingSystem tradingSystem);
	TradingSystem getTradingSystem();

	/**
	 * The Counter System Type
	 */
	//@Group(20)
	void setTradingCounterSystem(TradingSystem tradingSystem);
	TradingSystem getTradingCounterSystem();

	/**
	 * The Recv Time & date
	 * Child - NA
	 * Parent - FIX recv time 
	 */
	//@Group(21)
	void setRecvTime(long time);
	long getRecvTime();

	/**
	 * The Send Time & date
	 * Child - algo sent time
	 * Parent -  client sent time
	 */
	//@Group(21)
	void setSendTime(long time);
	long getSendTime();

	/**
	 * The Last Update Time
	 */
	//@Group(22)
	void setLastUpdateTime(long time);
	long getLastUpdateTime();

	/**
	 * The context key into the cache
	 * @param key
	 */
	//@Group(23)
	void setContextKey(long key);
	long getContextKey();

	/*
	 *  ********************* Drop Copy common fields for both Order & Execution Context ***********************
	 */

	/**
	 * The important Unique Id for the linking process in Drop Copy.
	 * This is a 12 bytes Globally Unique Id
	 */
	//@Group(24)
	void setUniqueId(@MaxUtf8Length(32) CharSequence uniqueId);
	CharSequence getUniqueId();
	StringBuilder getUsingUniqueId(StringBuilder uniqueId);

	/**
	 * The Important Unique Id of the Order parent for the child/execution -> parent in Drop Copy.
	 * This is a 12 bytes Globally Unique Id
	 * @param uniqueId
	 */
	//@Group(25)
	void setParentOrderUniqueId(@MaxUtf8Length(32) CharSequence uniqueId);
	CharSequence getParentOrderUniqueId();
	StringBuilder getUsingParentOrderUniqueId(StringBuilder uniqueId);

	/**
	 * The Unique Id of the previous order
	 * 12 bytes of unique id
	 * @param uniqueId
	 */
	//@Group(26)
	void setPreviousOrderUniqueId(@MaxUtf8Length(32) CharSequence uniqueId);
	CharSequence getPreviousOrderUniqueId();
	StringBuilder getUsingPreviousOrderUniqueId(StringBuilder uniqueId);

	/**
	 * The Unique Id of the corresponding conditional order placed
	 * 12 bytes of unique id
	 * @param uniqueId
	 */
	//@Group(27)
	void setConditionalOrderUniqueId(@MaxUtf8Length(32) CharSequence uniqueId);
	CharSequence getConditionalOrderUniqueId();
	StringBuilder getUsingConditionalOrderUniqueId(StringBuilder uniqueId);

	/**
	 * set the currency as mandated by the Drop copy specs
	 * @param senderCompId
	 */
	//@Group(28)
	void setCurrency(@MaxUtf8Length(64) CharSequence senderCompId);
	CharSequence getCurrency();
	StringBuilder getUsingCurrency(StringBuilder senderCompId);

	/*
	 * ****************** END Drop Copy Fields *******************
	 */

	/**
	 * The Symbol LN Id
	 * @param id
	 */
	//@Group(29)
	void setSymbolLnId(long id);
	long getSymbolLnId();

	/**
	 * The composite Symbol Ln Id
	 * @param id
	 */
	//@Group(30)
	void setSymbolCompositeLnId(long id);
	long getSymbolCompositeLnId();

	/**
	 * The Symbol
	 */
	//@Group(31)
	void setSymbol(@MaxUtf8Length(16) CharSequence symbol);
	CharSequence getSymbol();
	StringBuilder getUsingSymbol(StringBuilder symbol);

	/**
	 * set some text
	 * @param text
	 */
	//@Group(32)
	void setText(@MaxUtf8Length(64) CharSequence text);
	CharSequence getText();
	StringBuilder getUsingText(StringBuilder text);

	/**
	 * checks if this order is from warm up state of the container
	 * @param warmUp
	 */
	//@Group(33)
	void setWarmUp(boolean warmUp);
	boolean getWarmUp();

	/**
	 * Sets the Filled quantity of the order
	 * @param filled
	 */
	//@Group(34)
	void setFilledQuantity(int filled);
	int getFilledQuantity();

	/**
	 * Sets the Unfilled quantity of the order
	 * @param unfilled
	 */
	//@Group(35)
	void setUnfilledQuantity(int unfilled);
	int getUnfilledQuantity();

	/**
	 * Sets the Leaves quantity of the order
	 * @param leaves
	 */
	//@Group(36)
	void setLeavesQuantity(int leaves);
	int getLeavesQuantity();

	/**
	 * Sets the Unordered quantity of the order
	 * @param unordered
	 */
	//@Group(37)
	void setUnorderedQuantity(int unordered);
	int getUnorderedQuantity();

	/**
	 * Sets the Unacked quantity of the order
	 * @param unacked
	 */
	//@Group(38)
	void setUnackedQuantity(int unacked);
	int getUnackedQuantity();

	/**
	 * sets the filled value 
	 * @param filledValue
	 */
	//@Group(39)
	void setFilledValue(double filledValue);
	double getFilledValue();

	/**
	 * sets the average price
	 * @param avg
	 */
	//@Group(40)
	void setAveragePrice(double avg);
	double getAveragePrice();

	/**
	 * Sets the Min quantity of the firm order
	 * @param min
	 */
	//@Group(41)
	void setMinFirmQuantity(int min);
	int getMinFirmQuantity();

	/**
	 * Sets the Max quantity of the firm order
	 * @param max
	 */
	//@Group(42)
	void setMaxFirmQuantity(int max);
	int getMaxFirmQuantity();

	/**
	 * PriceInstruction
	 * @param priceInstruction
	 */
	//@Group(43)
	void setPriceInstruction(PriceInstruction priceInstruction);
	PriceInstruction getPriceInstruction();

	/**
	 * ATSMatchStatus
	 * @param matchStatus
	 */
	//@Group(44)
	void setATSMatchStatus(ATSMatchStatus matchStatus);
	ATSMatchStatus getATSMatchStatus();

	/**
	 * Request match status update from ATS
	 * @param statusUpdate
	 */
	//@Group(45)
	void setRequestMatchStatusUpdate(boolean statusUpdate);
	boolean getRequestMatchStatusUpdate();

	/**
	 * Enforce Min quantity on first fill
	 * @param enforceMinOnFirstFill
	 */
	//@Group(46)
	void setEnforceMinOnFirstFill(boolean enforceMinOnFirstFill);
	boolean getEnforceMinOnFirstFill();

	/**
	 * Can cancel or cancel/replace request be deferred
	 * @param isDeferrable
	 */
	//@Group(47)
	void setIsDeferrable(boolean isDeferrable);
	boolean getIsDeferrable();

	/**
	 * OrderRejectReason
	 */
	enum OrderRejectReason {
		NONE,
		UNKNOWN_SYMBOL,
		OUTSIDE_MKT_HOURS,
		INVALID_STATE,
		TLTC,
		OTHER
	}

	/**
	 * OrderRejectReason
	 * @param OrderRejectReason
	 */
	//@Group(48)
	void setOrderRejectReason(OrderRejectReason reason);
	OrderRejectReason getOrderRejectReason();

	/**
	 * CxlRejResponseTo
	 */
	enum CxlRejResponseTo {
		NONE,
		CANCEL_REQUEST,
		CANCEL_REPLACE_REQUEST
	}

	/**
	 * CxlRejResponseTo
	 * @param cxlRejResponseTo
	 */
	//@Group(49)
	void setCxlRejResponseTo(CxlRejResponseTo cxlRejResponseTo);
	CxlRejResponseTo getCxlRejResponseTo();

}
