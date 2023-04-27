package tn.talan.com.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;
import tn.talan.com.contracts.TokenContract;
import tn.talan.com.states.TokenState;

import java.util.Arrays;
import static java.util.Collections.singletonList;

/**
 * @author fozz101
 */
public class TokenIssueFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class TokenIssueFlowInitiator extends FlowLogic<SignedTransaction>{
        private final Party owner;
        private final int amount;

        public TokenIssueFlowInitiator(Party owner, int amount) {
            this.owner = owner;
            this.amount = amount;
        }
        private final ProgressTracker progressTracker = new ProgressTracker();
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }



        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            //select notary node
            final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"));
            Party issuer = getOurIdentity();

            TokenState tokenState = new TokenState(issuer, owner, amount);

            //build Tx
            TransactionBuilder transactionBuilder = new TransactionBuilder(notary)
                    .addOutputState(tokenState)
                    .addCommand(new TokenContract.Commands.Issue(), Arrays.asList(issuer.getOwningKey(), owner.getOwningKey()));

            //verify Tx based on implemented TokenContract
            transactionBuilder.verify(getServiceHub());

            FlowSession session = initiateFlow(owner);

            //Sign Tx
            SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

            //counterparty signs the transaction => fulltSignedTx
            SignedTransaction fullySignerTransaction = subFlow(new CollectSignaturesFlow(signedTransaction, singletonList(session)));

            // Tx notarised and recorded

            return subFlow(new FinalityFlow(fullySignerTransaction, singletonList(session)));
        }
        }

        @InitiatedBy(TokenIssueFlowInitiator.class)
        public static class TokenIssueFlowResponder extends FlowLogic<Void> {

            private FlowSession counterpartySession;

            public TokenIssueFlowResponder(FlowSession counterpartySession) {
                this.counterpartySession = counterpartySession;
            }

            @Suspendable
            @Override
            public Void call() throws FlowException {
                SignedTransaction signedTransaction = subFlow(new SignTransactionFlow(counterpartySession) {
                    @Suspendable
                    @Override
                    protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

                    }
                });

                //Store Tx
                subFlow(new ReceiveFinalityFlow(counterpartySession, signedTransaction.getId()));
                return null;
            }
        }

    }

