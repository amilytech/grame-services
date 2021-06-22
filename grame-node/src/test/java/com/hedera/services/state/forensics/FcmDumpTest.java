package com.grame.services.state.forensics;

import com.grame.services.ServicesState;
import com.grame.services.state.merkle.MerkleAccount;
import com.grame.services.state.merkle.MerkleBlobMeta;
import com.grame.services.state.merkle.MerkleEntityAssociation;
import com.grame.services.state.merkle.MerkleEntityId;
import com.grame.services.state.merkle.MerkleOptionalBlob;
import com.grame.services.state.merkle.MerkleSchedule;
import com.grame.services.state.merkle.MerkleToken;
import com.grame.services.state.merkle.MerkleTokenRelStatus;
import com.grame.services.state.merkle.MerkleTopic;
import com.swirlds.common.NodeId;
import com.swirlds.common.merkle.MerkleNode;
import com.swirlds.common.merkle.io.MerkleDataOutputStream;
import com.swirlds.fcmap.FCMap;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FcmDumpTest {
	long selfId = 1, round = 1_234_567;
	NodeId self = new NodeId(false, selfId);

	@Mock
	Logger mockLog;
	@Mock
	ServicesState state;
	@Mock
	MerkleDataOutputStream out;
	@Mock
	Function<String, MerkleDataOutputStream> merkleOutFn;
	@Mock
	FCMap<MerkleEntityId, MerkleAccount> accounts;
	@Mock
	FCMap<MerkleBlobMeta, MerkleOptionalBlob> storage;
	@Mock
	FCMap<MerkleEntityId, MerkleTopic> topics;
	@Mock
	FCMap<MerkleEntityId, MerkleToken> tokens;
	@Mock
	FCMap<MerkleEntityAssociation, MerkleTokenRelStatus> tokenAssociations;
	@Mock
	FCMap<MerkleEntityId, MerkleSchedule> scheduleTxs;

	FcmDump subject = new FcmDump();

	@Test
	void dumpsAllFcms() throws IOException {
		// setup:
		FcmDump.merkleOutFn = merkleOutFn;

		given(merkleOutFn.apply(any())).willReturn(out);
		// and:
		given(state.accounts()).willReturn(accounts);
		given(state.storage()).willReturn(storage);
		given(state.topics()).willReturn(topics);
		given(state.tokens()).willReturn(tokens);
		given(state.tokenAssociations()).willReturn(tokenAssociations);
		given(state.scheduleTxs()).willReturn(scheduleTxs);
//		// and:
//		willThrow(IOException.class).given(out).writeMerkleTree(scheduleTxs);

		// when:
		subject.dumpFrom(state, self, round);

		// then:
		verify(out).writeMerkleTree(accounts);
		verify(out).writeMerkleTree(storage);
		verify(out).writeMerkleTree(topics);
		verify(out).writeMerkleTree(tokens);
		verify(out).writeMerkleTree(tokenAssociations);
		verify(out).writeMerkleTree(scheduleTxs);
		// and:
		verify(out, times(6)).close();
		// and: verify(mockLog).warn(String.format(FcmDump.DUMP_IO_WARNING, "scheduleTxs"));
	}

	@Test
	void recoversToKeepTryingDumps() throws IOException {
		// setup:
		FcmDump.log = mockLog;
		FcmDump.merkleOutFn = merkleOutFn;

		given(merkleOutFn.apply(any())).willReturn(out);
		// and:
		given(state.accounts()).willReturn(accounts);
		given(state.storage()).willReturn(storage);
		given(state.topics()).willReturn(topics);
		given(state.tokens()).willReturn(tokens);
		given(state.tokenAssociations()).willReturn(tokenAssociations);
		given(state.scheduleTxs()).willReturn(scheduleTxs);
		// and:
		willThrow(IOException.class).given(out).writeMerkleTree(any());

		// when:
		subject.dumpFrom(state, self, round);

		// then:
		verify(mockLog).warn(String.format(FcmDump.DUMP_IO_WARNING, "accounts"));
		verify(mockLog).warn(String.format(FcmDump.DUMP_IO_WARNING, "storage"));
		verify(mockLog).warn(String.format(FcmDump.DUMP_IO_WARNING, "topics"));
		verify(mockLog).warn(String.format(FcmDump.DUMP_IO_WARNING, "tokens"));
		verify(mockLog).warn(String.format(FcmDump.DUMP_IO_WARNING, "tokenAssociations"));
		verify(mockLog).warn(String.format(FcmDump.DUMP_IO_WARNING, "scheduleTxs"));
	}

	@Test
	public void merkleSupplierWorks() {
		// given:
		var okPath = "src/test/resources/tmp.nothing";

		// when:
		var fout = FcmDump.merkleOutFn.apply(okPath);
		// and:
		assertDoesNotThrow(() -> fout.writeUTF("Here is something"));

		// cleanup:
		(new File(okPath)).delete();
	}

	@Test
	public void merkleSupplierFnDoesntBlowUp() {
		// given:
		var badPath = "/impermissible/path";

		// then:
		assertDoesNotThrow(() -> FcmDump.merkleOutFn.apply(badPath));
	}
}