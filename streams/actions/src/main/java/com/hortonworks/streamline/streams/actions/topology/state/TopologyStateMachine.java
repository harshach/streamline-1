package com.hortonworks.streamline.streams.actions.topology.state;

import java.util.Collection;

/**
 * The starting and deployed states of a topology state-machine
 * implementation. This can be extended to customize the state machine
 * implementation.
 */
public interface TopologyStateMachine {
  /**
   * Returns the initial state
   *
   * @return the initial state
   */
  TopologyState initialState();

  /**
   * Returns the deployed state
   *
   * @return the deployed state
   */
  TopologyState deployedState();

  /**
   * Return all states of this state machine
   *
   * @return all possible states in this state machine
   */
  Collection<TopologyState> allStates();

  /**
   * Get TopologyState from a state name
   *
   * @return TopologyState for the input stateName
   */
  TopologyState getTopologyState(String stateName);
}
