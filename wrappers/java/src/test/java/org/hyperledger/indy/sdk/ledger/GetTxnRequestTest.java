package org.hyperledger.indy.sdk.ledger;

import org.hyperledger.indy.sdk.IndyIntegrationTest;
import org.hyperledger.indy.sdk.pool.Pool;
import org.hyperledger.indy.sdk.signus.Signus;
import org.hyperledger.indy.sdk.signus.SignusJSONParameters;
import org.hyperledger.indy.sdk.signus.SignusResults;
import org.hyperledger.indy.sdk.utils.PoolUtils;
import org.hyperledger.indy.sdk.wallet.Wallet;
import org.json.JSONObject;
import org.junit.*;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GetTxnRequestTest extends IndyIntegrationTest {

	private Pool pool;
	private Wallet wallet;
	private String walletName = "ledgerWallet";

	@Rule
	public Timeout globalTimeout = new Timeout(5, TimeUnit.SECONDS);

	@Before
	public void openPool() throws Exception {
		String poolName = PoolUtils.createPoolLedgerConfig();
		pool = Pool.openPoolLedger(poolName, null).get();

		Wallet.createWallet(poolName, walletName, "default", null, null).get();
		wallet = Wallet.openWallet(walletName, null, null).get();
	}

	@After
	public void closePool() throws Exception {
		pool.closePoolLedger().get();
		wallet.closeWallet().get();
		Wallet.deleteWallet(walletName, null).get();
	}

	@Test
	public void testBuildGetTxnRequestWorks() throws Exception {

		String identifier = "Th7MpTaRZVRYnPiabds81Y";
		int data = 1;

		String expectedResult = String.format("\"identifier\":\"%s\"," +
				"\"operation\":{" +
				"\"type\":\"3\"," +
				"\"data\":%s" +
				"}", identifier, data);

		String getTxnRequest = Ledger.buildGetTxnRequest(identifier, data).get();

		assertTrue(getTxnRequest.replace("\\", "").contains(expectedResult));
	}

	@Test
	public void testGetTxnRequestWorks() throws Exception {

		SignusJSONParameters.CreateAndStoreMyDidJSONParameter trusteeDidJson =
				new SignusJSONParameters.CreateAndStoreMyDidJSONParameter(null, "000000000000000000000000Trustee1", null, null);

		SignusResults.CreateAndStoreMyDidResult didResult = Signus.createAndStoreMyDid(wallet, trusteeDidJson.toJson()).get();
		String did = didResult.getDid();

		String schemaData = "{\"name\":\"gvt2\",\"version\":\"3.0\",\"keys\": [\"name\", \"male\"]}";

		String schemaRequest = Ledger.buildSchemaRequest(did, schemaData).get();
		String schemaResponse = Ledger.signAndSubmitRequest(pool, wallet, did, schemaRequest).get();

		JSONObject schemaResponseObj = new JSONObject(schemaResponse);

		int seqNo = schemaResponseObj.getJSONObject("result").getInt("seqNo");

		String getTxnRequest = Ledger.buildGetTxnRequest(did, seqNo).get();
		String getTxnResponse = Ledger.submitRequest(pool, getTxnRequest).get();

		JSONObject getTxnResponseObj = new JSONObject(getTxnResponse);

		String schemaTransaction = getTxnResponseObj.getJSONObject("result").getString("data");
		JSONObject schemaTransactionObj = new JSONObject(schemaTransaction);

		assertEquals(schemaData, schemaTransactionObj.getString("data"));
	}

	@Test
	public void testGetTxnRequestWorksForInvalidSeqNo() throws Exception {

		SignusJSONParameters.CreateAndStoreMyDidJSONParameter trusteeDidJson =
				new SignusJSONParameters.CreateAndStoreMyDidJSONParameter(null, "000000000000000000000000Trustee1", null, null);

		SignusResults.CreateAndStoreMyDidResult didResult = Signus.createAndStoreMyDid(wallet, trusteeDidJson.toJson()).get();
		String did = didResult.getDid();

		String schemaData = "{\"name\":\"gvt2\",\"version\":\"3.0\",\"keys\": [\"name\", \"male\"]}";

		String schemaRequest = Ledger.buildSchemaRequest(did, schemaData).get();
		String schemaResponse = Ledger.signAndSubmitRequest(pool, wallet, did, schemaRequest).get();

		JSONObject schemaResponseObj = new JSONObject(schemaResponse);

		int seqNo = schemaResponseObj.getJSONObject("result").getInt("seqNo") + 1;

		String getTxnRequest = Ledger.buildGetTxnRequest(did, seqNo).get();
		String getTxnResponse = Ledger.submitRequest(pool, getTxnRequest).get();

		JSONObject getTxnResponseObj = new JSONObject(getTxnResponse);

		String schemaTransaction = getTxnResponseObj.getJSONObject("result").getString("data");
		assertEquals("{}", schemaTransaction);
	}
}
