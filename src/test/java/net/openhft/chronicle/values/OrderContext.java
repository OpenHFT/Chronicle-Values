package net.openhft.chronicle.values;

import net.openhft.chronicle.bytes.BytesStore;

import java.nio.ByteBuffer;

@SuppressWarnings("all")
public interface OrderContext extends CommonContext {
	int MAX_SIZE = 16;
//	int ORDER_GROUP_OFFSET = 50;

	/**
	 * The native reference for the Order Context
	 * @return
	 */
	static OrderContext nativeReference() {
		OrderContext orderDataContext = Values.newNativeReference(OrderContext.class);
		long maxSize = orderDataContext.maxSize();
		BytesStore bytesStore = BytesStore.wrap(ByteBuffer.allocateDirect((int)maxSize));
		bytesStore.zeroOut(0, maxSize);
		orderDataContext.bytesStore(bytesStore, 0L, maxSize);
		orderDataContext.setContextType(ContextType.ORDER);
		return orderDataContext;
	}

	/**
	 * The native reference using a wrapped Byte Buffer
	 * @param byteBuffer
	 * @return
	 */
	static OrderContext nativeReference(ByteBuffer byteBuffer) {
		OrderContext orderDataContext = Values.newNativeReference(OrderContext.class);
		long maxSize = orderDataContext.maxSize();
		if(maxSize > byteBuffer.capacity()) {
			throw new IllegalArgumentException("The Capacity of the Byte Buffer is insufficient, it should atleas [" + maxSize + "]");
		}
		BytesStore bytesStore = BytesStore.wrap(byteBuffer);
		orderDataContext.bytesStore(bytesStore, 0L, maxSize);
		orderDataContext.setContextType(ContextType.ORDER);
		return orderDataContext;
	}

	/**
	 * returns the shallow reference for using in get/acquire in caches
	 * @return
	 */
	static OrderContext shallowReference() {
		return Values.newNativeReference(OrderContext.class);
	}

	/**
	 * OrderMsgType
	 */
	enum OrderMsgType {
		NEW,
		CANCEL,
		REPLACE,
		REJECT,
		CANCEL_REJECT
	}


	/**
	 * The Head Alive Child Order Id
	 */
	//@Group(2)
	long getHeadAliveChildOrderId();
	void setHeadAliveChildOrderId(long id);


	/**
	 * OrderMsgType
	 * @param OrderMsgType
	 */
	//@Group(0)
	void setOrderMsgType(OrderMsgType msgType);
	OrderMsgType getOrderMsgType();

	/**
	 * Alive
	 * @param alive
	 */
	//@Group(1)
	void setAlive(boolean flag);
	boolean getAlive();

	/**
	 * Number of Child Orders in Pending State waiting to become Active
	 * @param num
	 */
	//@Group(2)
	void setNumPendingChildOrders(int num);
	int getNumPendingChildOrders();

	/**
	 * Number of Conditional Orders in Pending State waiting to become Active
	 * @param num
	 */
	//@Group(3)
	void setNumPendingConditionalOrders(int num);
	int getNumPendingConditionalOrders();

	/**
	 * The Cancel Order Id (Cancel/Replace order)
	 * @param id
	 */
	//@Group(4)
	void setCancelOrderId(long id);
	long getCancelOrderId();

	/**
	 * The Parent Order Id
	 * @param id
	 */
	//@Group(5)
	void setParentOrderId(long id);
	long getParentOrderId();

	/**
	 * The Conditional Order Id
	 * @param id
	 */
	//@Group(6)
	void setConditionalOrderId(long id);
	long getConditionalOrderId();

	/**
	 * The Firming-Up Conditional Order Id
	 * @param id
	 */
	//@Group(7)
	void setFirmingUpConditionalOrderId(long id);
	long getFirmingUpConditionalOrderId();

	/**
	 * The Next Repalce Order Id
	 * @param id
	 */
	//@Group(8)
	void setNextOrderId(long id);
	long getNextOrderId();

	/**
	 * The Previous Repalce Order Id
	 * @param id
	 */
	//@Group(9)
	void setPrevOrderId(long id);
	long getPrevOrderId();




	/**
	 * The Next Alive Child Order Id
	 * @param id
	 */
	//@Group(11)
	void setNextAliveChildOrderId(long id);
	long getNextAliveChildOrderId();

	/**
	 * The Prev Alive Child Order Id
	 * @param id
	 */
	//@Group(12)
	void setPrevAliveChildOrderId(long id);
	long getPrevAliveChildOrderId();

	/**
	 * The Head Conditional Order Id
	 * @param id
	 */
	//@Group(13)
	void setHeadConditionalOrderId(long id);
	long getHeadConditionalOrderId();

	/**
	 * The Next Conditional Order Id
	 * @param id
	 */
	//@Group(14)
	void setNextConditionalOrderId(long id);
	long getNextConditionalOrderId();

	/**
	 * The Prev Conditional Order Id
	 * @param id
	 */
	//@Group(15)
	void setPrevConditionalOrderId(long id);
	long getPrevConditionalOrderId();

	/**
	 * The Firm Child Order Id
	 * @param id
	 */
	//@Group(16)
	void setFirmChildOrderId(long id);
	long getFirmChildOrderId();

	/**
	 * The Firmup Request Id
	 * @param id
	 */
	//@Group(17)
	void setFirmupRequestId(long id);
	long getFirmupRequestId();

	/**
	 * OnFirmUpConditionalActionType
	 */
	enum OnFirmUpConditionalActionType {
		DEFAULT,
		OUT_ON_FIRM_UP_REQUEST,
		OUT_ON_FIRM_ORDER_FILLED,
		CANCEL_ON_FIRM_ORDER_FILLED
	}

	/**
	 * OnFirmUpConditionalActionType
	 * @param onFirmUpConditionalActionType
	 */
	//@Group(18)
	void setOnFirmUpConditionalActionType(OnFirmUpConditionalActionType type);
	OnFirmUpConditionalActionType getOnFirmUpConditionalActionType();

	/**
	 * StrategyDestinationType
	 */
	enum StrategyDestinationType {
		DMA,
		ALGO,
		DARK,
		OTHER
	}

	/**
	 * StrategyDestinationType
	 * @param StrategyDestinationType
	 */
	//@Group(19)
	void setStrategyDestinationType(StrategyDestinationType type);
	StrategyDestinationType getStrategyDestinationType();

	/**
	 *  StrategyDestination
	 */
	//@Group(20)
	void setStrategyDestination(@MaxUtf8Length(32) CharSequence strategyDestination);
	CharSequence getStrategyDestination();
	StringBuilder getUsingStrategyDestination(StringBuilder strategyDestination);

	/**
	 * Strategy Id
	 * @param id
	 */
	//@Group(21)
	void setStrategyId(long id);
	long getStrategyId();

	/**
	 * Destination Id
	 * @param id
	 */
	//@Group(22)
	void setDestinationId(long id);
	long getDestinationId();

	/**
	 *  Parent Strategy Destination
	 */
	//@Group(23)
	void setParentStrategyDestination(@MaxUtf8Length(32) CharSequence parentStrategyDestination);
	CharSequence getParentStrategyDestination();
	StringBuilder getUsingParentStrategyDestination(StringBuilder parentStrategyDestination);

	/**
	 * Client Name
	 */
	//@Group(24)
	void setClient(@MaxUtf8Length(16) CharSequence client);
	CharSequence getClient();
	StringBuilder getUsingClient(StringBuilder client);

	/**
	 * Client Id
	 * @param id
	 */
	//@Group(25)
	void setClientId(long id);
	long getClientId();

	/*
	 * TimeInForce
	 */
	enum TimeInForce {
		DAY,
		GOOD_TIL_CANCEL,
		AT_THE_OPENING,
		IOC,
		FOK,
		AT_THE_CLOSE
	}

	/**
	 * TimeInForce
	 * @param TimeInForce
	 */
	//@Group(27)
	void setTimeInForce(TimeInForce t);
	TimeInForce getTimeInForce();

	/**
	 * Sets the Min quantity of the order
	 * @param min
	 */
	//@Group(28)
	void setMinQuantity(int min);
	int getMinQuantity();

	/**
	 * Sets the Recurring Min quantity of the order
	 * @param min
	 */
	//@Group(29)
	void setRecurringMinQuantity(int min);
	int getRecurringMinQuantity();

	/**
	 * Sets the Display quantity of the order
	 * @param display
	 */
	//@Group(30)
	void setDisplayQuantity(int display);
	int getDisplayQuantity();

	/**
	 * Originating System
	 * @param originatingSystem
	 */
	//@Group(31)
	void setOriginatingSystem(@MaxUtf8Length(32) CharSequence originatingSystem);
	CharSequence getOriginatingSystem();
	StringBuilder getUsingOriginatingSystem(StringBuilder text);

	/**
	 * Reason
	 */
	//@Group(32)
	void setReason(@MaxUtf8Length(256) CharSequence text);
	CharSequence getReason();
	StringBuilder getUsingReason(StringBuilder text);

	/*
	 * ************************ Order specific The Drop copy fields **********************
	 */

	/**
	 * The Unique Id of the firm up request
	 * 12 bytes of unique id
	 * @param uniqueId
	 */
	//@Group(33)
	void setFirmupRequestUniqueId(@MaxUtf8Length(32) CharSequence uniqueId);
	CharSequence getFirmupRequestUniqueId();
	StringBuilder getUsingFirmupRequestUniqueId(StringBuilder uniqueId);


	/*
	 *********************** Strategy parameters ***********************
	 */

	enum WaveSizeType {
		PERCENT_TGT_QTY {
			/**
			 * returns the Wave Quantity
			 *
			 * @param waveSize
			 * @param targetQty
			 * @return
			 */
			@Override
			public long getWaveQuantity(long waveSize, long targetQty, double price) {
				return (targetQty*waveSize)/100;
			}
		},
		SHARES {
			/**
			 * returns the Wave Quantity
			 *
			 * @param waveSize
			 * @param targetQty
			 * @return
			 */
			@Override
			public long getWaveQuantity(long waveSize, long targetQty, double price) {
				return Math.min(waveSize,targetQty);
			}
		},
		VALUE {
			/**
			 * returns the Wave Quantity
			 *
			 * @param waveSize
			 * @param targetQty
			 * @return
			 */
			@Override
			public long getWaveQuantity(long waveSize, long targetQty, double price) {
				return (long) (waveSize/price);
			}
		};

		/**
		 * returns the Wave Quantity
		 * @param waveSize
		 * @param targetQty
		 * @return
		 */
		public abstract long getWaveQuantity(long waveSize, long targetQty, double price);
	}

	/**
	 * The Wave Size Type
	 * @return
	 */
	//@Group(34)
	WaveSizeType getWaveSizeType();
	void setWaveSizeType(WaveSizeType waveSizeType);

	/**
	 * The Wave Size
	 * @return
	 */
	//@Group(35)
	long getWaveSize();
	void setWaveSize(long size);

	/**
	 * The Wave Price
	 * @return
	 */
	//@Group(36)
	double getWavePrice();
	void setWavePrice(double price);

	/**
	 * Order Start Time
	 * @param time
	 */
	//@Group(37)
	void setStartTime(long time);
	long getStartTime();

	/**
	 * Order End Time
	 * @param time
	 */
	//@Group(38)
	void setEndTime(long time);
	long getEndTime();


	/**
	 * Min POV
	 * @param pov
	 */
	//@Group(39)
	void setMinPOV(int pov);
	int getMinPOV();

	/**
	 * Adjusted Min POV
	 * @param pov
	 */
	//@Group(40)
	void setAdjustedMinPOV(int pov);
	int getAdjustedMinPOV();

	/**
	 * Max POV
	 * @param pov
	 */
	//@Group(41)
	void setMaxPOV(int pov);
	int getMaxPOV();

	/**
	 * Adjusted Max POV
	 * @param pov
	 */
	//@Group(42)
	void setAdjustedMaxPOV(int pov);
	int getAdjustedMaxPOV();

	/**
	 * Lit POV
	 * @param pov
	 */
	//@Group(43)
	void setLitPOV(int pov);
	int getLitPOV();

	/**
	 * Adjusted Lit POV
	 * @param pov
	 */
	//@Group(44)
	void setAdjustedLitPOV(int pov);
	int getAdjustedLitPOV();

	/**
	 * Auction POV
	 * @param pov
	 */
	//@Group(45)
	void setAuctionPOV(int pov);
	int getAuctionPOV();

	/**
	 * Open Auction POV
	 * @param pov
	 */
	//@Group(46)
	void setOpenAuctionPOV(int pov);
	int getOpenAuctionPOV();

	/**
	 * Urgency
	 * @param urgency
	 */
	//@Group(47)
	void setUrgency(int urgency);
	int getUrgency();

	/**
	 * Open Auction
	 * @param flag
	 */
	//@Group(48)
	void setOpenAuction(char flag);
	char getOpenAuction();

	/**
	 * Close Auction
	 * @param flag
	 */
	//@Group(49)
	void setCloseAuction(char flag);
	char getCloseAuction();

	/**
	 * Complete
	 * @param flag
	 */
	//@Group(50)
	void setComplete(char flag);
	char getComplete();

	/**
	 * Targeted Invitation
	 * @param flag
	 */
	//@Group(51)
	void setTargetedInvitation(char flag);
	char getTargetedInvitation();

	/**
	 * Dynamic Targeted Invitation
	 * @param flag
	 */
	//@Group(52)
	void setDynamicTargetedInvitation(boolean flag);
	boolean getDynamicTargetedInvitation();

	/**
	 * Post Control
	 * @param control
	 */
	//@Group(53)
	void setPostControl(char control);
	char getPostControl();

	/**
	 * Interlisted Trading
	 * @param flag
	 */
	//@Group(54)
	void setInterlistedTrading(char flag);
	char getInterlistedTrading();

	/**
	 * Contract For Diff
	 * @param cfd
	 */
	//@Group(55)
	void setContractForDiff(char cfd);
	char getContractForDiff();

	/**
	 * Ignore StartTime
	 * @param flag
	 */
	//@Group(56)
	void setIgnoreStartTime(boolean flag);
	boolean getIgnoreStartTime();

	/**
	 * Dark Mode
	 * @param mode
	 */
	//@Group(57)
	void setDarkMode(@MaxUtf8Length(32) CharSequence mode);
	CharSequence getDarkMode();
	StringBuilder getUsingDarkMode(StringBuilder mode);

	/**
	 * List ID
	 * @param id
	 */
	//@Group(58)
	void setListID(@MaxUtf8Length(64) CharSequence id);
	CharSequence getListID();
	StringBuilder getUsingListID(StringBuilder id);

	/**
	 * Benchmark
	 * @param benchmark
	 */
	//@Group(59)
	void setBenchmark(int benchmark);
	int getBenchmark();

	/**
	 * LN Pool Exposure
	 * @param exposure
	 */
	//@Group(60)
	void setLNPoolExposure(int exposure);
	int getLNPoolExposure();

	/**
	 * LN Block Filter
	 * @param filter
	 */
	//@Group(61)
	void setLNBlockFilter(long filter);
	long getLNBlockFilter();

	/**
	 * Long Short Ratio
	 * @param ratio
	 */
	//@Group(62)
	void setLongShortRatio(float ratio);
	float getLongShortRatio();

	/**
	 * Max Raised
	 * @param raised
	 */
	//@Group(63)
	void setMaxRaised(float raised);
	float getMaxRaised();

	/**
	 * Max Spent
	 * @param spent
	 */
	//@Group(64)
	void setMaxSpent(float spent);
	float getMaxSpent();

	/**
	 * Dark Only
	 * @param flag
	 */
	//@Group(65)
	void setDarkOnly(char flag);
	char getDarkOnly();




	/**
	 * Ext I Would Price
	 * @param limit
	 */
	//@Group(67)
	void setExtIWouldPrice(float limit);
	float getExtIWouldPrice();


	/*
	 * ******************** LN I Would feature and ATS pool participation controls *****************
	 */

	/**
	 * LN I Would Quantity
	 * @param quantity
	 */
	//@Group(68)
	void setLNIWouldQuantity(long quantity);
	long getLNIWouldQuantity();


	//@Group(1)
	long getExtIWouldQuantity();
	void setExtIWouldQuantity(long quantity);

	/**
	 * LN I Would Percent
	 * @param percent
	 */
	//@Group(69)
	void setLNIWouldPercent(int percent);
	int getLNIWouldPercent();

	/**
	 * LN I Would Min Quantity
	 * @param quantity
	 */
	//@Group(70)
	void setLNIWouldMinQuantity(long quantity);
	long getLNIWouldMinQuantity();

	/**
	 * LN I Would Price
	 * @param limit
	 */
	//@Group(71)
	void setLNIWouldPrice(float limit);
	float getLNIWouldPrice();

	/**
	 * Should Send IOIs
	 * @param flag
	 */
	//@Group(72)
	void setShouldSendIOIs(char flag);
	char getShouldSendIOIs();

	/**
	 * Can Cross SLP IOC
	 * @param flag
	 */
	//@Group(73)
	void setCanCrossSLPIOC(char flag);
	char getCanCrossSLPIOC();

	/**
	 * Can Cross SLP Day
	 * @param flag
	 */
	//@Group(74)
	void setCanCrossSLPDay(char flag);
	char getCanCrossSLPDay();

	/**
	 * Can Cross Internal
	 * @param flag
	 */
	//@Group(75)
	void setCanCrossInternal(char flag);
	char getCanCrossInternal();


	/*
	 * ******************** Outbound parameters *****************
	 */

	/**
	 * Algo Street Quantity
	 * @param quantity
	 */
	//@Group(76)
	void setAlgoStreetQuantity(long quantity);
	long getAlgoStreetQuantity();

	/**
	 * Algo Street Price
	 * @param limit
	 */
	//@Group(77)
	void setAlgoStreetPrice(float limit);
	float getAlgoStreetPrice();

	/*
	 * ******************** Destination specific parameters *****************
	 */

	/*
	 * ******************** Some Internal Fields to maintain the OMS status *****************
	 */

	/**
	 * Internal OMS status
	 */
	enum OMSStatus {
		ACCEPT, WARNING, ERROR
	}

	/**
	 * The processing OMS status.
	 * Used for internal purposes
	 * @param status
	 */
	//@Group(79)
	void setStatus(OMSStatus status);
	OMSStatus getStatus();
}
